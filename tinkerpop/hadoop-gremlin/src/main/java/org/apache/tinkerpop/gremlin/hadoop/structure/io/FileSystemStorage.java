/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.tinkerpop.gremlin.hadoop.structure.io;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.tinkerpop.gremlin.hadoop.Constants;
import org.apache.tinkerpop.gremlin.hadoop.structure.HadoopGraph;
import org.apache.tinkerpop.gremlin.process.computer.KeyValue;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.io.Storage;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public final class FileSystemStorage implements Storage {

    private static final String SPACE = " ";
    private static final String D_SPACE = "(D) ";

    private final FileSystem fs;

    private FileSystemStorage(final FileSystem fileSystem) {
        this.fs = fileSystem;
    }

    public static FileSystemStorage open() {
        return FileSystemStorage.open(new Configuration());
    }

    public static FileSystemStorage open(final Configuration configuration) {
        try {
            return new FileSystemStorage(FileSystem.get(configuration));
        } catch (final IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public static FileSystemStorage open(final FileSystem fileSystem) {
        return new FileSystemStorage(fileSystem);
    }

    private static String fileStatusString(final FileStatus status) {
        StringBuilder s = new StringBuilder();
        s.append(status.getPermission()).append(SPACE);
        s.append(status.getOwner()).append(SPACE);
        s.append(status.getGroup()).append(SPACE);
        s.append(status.getLen()).append(SPACE);
        if (status.isDirectory())
            s.append(D_SPACE);
        s.append(status.getPath().getName());
        return s.toString();
    }

    private String tryHomeDirectory(final String location) {
        return location.equals(ROOT_DIRECTORY) ? this.fs.getHomeDirectory().toString() : location;
    }

    @Override
    public List<String> ls() {
        return this.ls(ROOT_DIRECTORY);
    }

    @Override
    public List<String> ls(final String pattern) {
        return Stream.of(this.listStatuses(pattern)).map(FileSystemStorage::fileStatusString).collect(Collectors.toList());
    }

    /**
     * @param pattern refers directory or a set of files
     * @return a non-null array of the files and directories the pattern refers as of {@link Storage#ls(String)}
     */
    private FileStatus[] listStatuses(final String pattern) throws IllegalStateException {
        final FileStatus[] result;
        final String actualPattern;

        actualPattern = tryHomeDirectory(pattern);

        try {
            if ( fs.isDirectory(new Path(actualPattern)) ) { // a directory path - list its contents
                if ( actualPattern.endsWith( "/" ) ) {
                    result = fs.globStatus(new Path(actualPattern + "*"));
                } else {
                    result = fs.globStatus(new Path(actualPattern + "/*"));
                }
            } else { // a pattern or file path
                result = fs.globStatus(new Path(actualPattern));
            }
        } catch (final IOException e) {
          throw new IllegalStateException(e.getMessage(), e);
        }
        return result == null ? new FileStatus[0] : result;
    }

    public boolean mkdir(final String location) {
        try {
            return this.fs.mkdirs(new Path(location));
        } catch (final IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public boolean cp(final String sourceLocation, final String targetLocation) {
        try {
            return FileUtil.copy(this.fs, new Path(sourceLocation), this.fs, new Path(targetLocation), false, new Configuration());
        } catch (final IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public boolean exists(final String location) {
        return this.listStatuses(location).length > 0;
    }

    @Override
    public boolean rm(final String location) {
        try {
            final FileStatus[] statuses = this.fs.globStatus(new Path(tryHomeDirectory(location)));
            return statuses != null
                   && statuses.length > 0
                   && Stream.of(statuses).allMatch(status -> {
                          try {
                              return this.fs.delete(status.getPath(), true);
                          } catch (IOException e) {
                              throw new IllegalStateException(e.getMessage(), e);
                          }
                      });
        } catch (final IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public Iterator<String> head(final String location, final int totalLines) {
        try {
            return IteratorUtils.limit((Iterator) new TextIterator(this.fs.getConf(), new Path(location)), totalLines);
        } catch (final IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public Iterator<Vertex> head(final String location, final Class readerClass, final int totalLines) {
        final org.apache.commons.configuration.Configuration configuration = new BaseConfiguration();
        configuration.setProperty(Constants.GREMLIN_HADOOP_INPUT_LOCATION, Constants.getSearchGraphLocation(location, this).get());
        configuration.setProperty(Constants.GREMLIN_HADOOP_GRAPH_READER, readerClass.getCanonicalName());
        try {
            if (InputFormat.class.isAssignableFrom(readerClass))
                return IteratorUtils.limit(new HadoopVertexIterator(HadoopGraph.open(configuration)), totalLines);
        } catch (final IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        throw new IllegalArgumentException("The provided parser class must be an " + InputFormat.class.getCanonicalName() + ": " + readerClass.getCanonicalName());

    }

    @Override
    public <K, V> Iterator<KeyValue<K, V>> head(final String location, final String memoryKey, final Class readerClass, final int totalLines) {
        if (!readerClass.equals(SequenceFileInputFormat.class))
            throw new IllegalArgumentException("Only " + SequenceFileInputFormat.class.getCanonicalName() + " memories are supported");
        final Configuration configuration = new Configuration();
        try {
            return IteratorUtils.limit((Iterator) new ObjectWritableIterator(configuration, new Path(Constants.getMemoryLocation(location, memoryKey))), totalLines);
        } catch (final IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public String toString() {
        return StringFactory.storageString(this.fs.toString());
    }

    /////////

    public void copyToLocal(final String fromLocation, final String toLocation) {
        try {
            this.fs.copyToLocalFile(new Path(fromLocation), new Path(toLocation));
        } catch (final IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public void copyFromLocal(final String fromLocation, final String toLocation) {
        try {
            this.fs.copyFromLocalFile(new Path(fromLocation), new Path(toLocation));
        } catch (final IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public void mergeToLocal(final String fromLocation, final String toLocation) {
        try (final FileSystem local = FileSystem.getLocal(new Configuration())) {
            try (final FSDataOutputStream outA = local.create(new Path(toLocation))) {
                final Path fromPath;

                if (null == fromLocation || fromLocation.equals(ROOT_DIRECTORY) ) {
                    fromPath = fs.getHomeDirectory();
                } else {
                    fromPath = new Path(fromLocation);
                }

                for (final Path path : FileSystemStorage.getAllFilePaths(fs, fromPath, HiddenFileFilter.instance())) {
                    try (final FSDataInputStream inA = fs.open(path) ) {
                        IOUtils.copyBytes(inA, outA, 8192);
                    }
                }
            }
        } catch (final IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }


    private static List<Path> getAllFilePaths(final FileSystem fs, Path path, final PathFilter filter) throws IOException {
        final List<Path> paths = new ArrayList<Path>();

        if (fs.isFile(path)) {
            paths.add(path);
        } else {
            for (final FileStatus status : fs.globStatus(new Path(path, "*"), filter)) {
                paths.addAll(getAllFilePaths(fs, status.getPath(), filter));
            }
        }
        return paths;
    }
}

const moduleName = 'kgraph';

module.exports = {
  title: '知识图谱',
  context: '',
  pathInMappingJson: '/',
  moduleName,
  entry: {
    vendor: [
      'prop-types', 'react', 'react-dom', 'react-router-dom', 'common-upload', 'ocean-utils'
    ],
    [moduleName]: ['./src/index.js']
  }
}

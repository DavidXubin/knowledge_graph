const path = require('path');
const webpack = require('webpack');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const TerserPlugin = require('terser-webpack-plugin');
const OptimizeCSSAssetsPlugin = require('optimize-css-assets-webpack-plugin');
// const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin;
const ManifestPlugin = require('webpack-manifest-plugin');
const HappyPack = require('happypack');
const config = require('./config');

module.exports = {
  mode: 'production',
  entry: config.entry,
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src')
    },
    extensions: ['.js', '.jsx', '.less', '.png', '.jpg', '.gif'],
  },
  output: {
    path: path.join(__dirname, 'public'),
    publicPath: config.pathInMappingJson,
    filename: '[name].[chunkhash].js',
    sourceMapFilename: 'map/[file].map',
  },
  optimization: {
    minimizer: [
      new TerserPlugin({
        cache: true,
        parallel: true,
        sourceMap: false,
        terserOptions: {
          warnings: false
        }
      }),
      new OptimizeCSSAssetsPlugin({})
    ]
  },
  devtool: 'source-map',
  module: {
    rules: [
      {
        test: /\.js(x?)$/,
        include: path.join(__dirname, 'src'),
        loader: 'babel-loader',
      },
      {
        test: /\.css$/,
        use: [
          {
            loader: MiniCssExtractPlugin.loader
          },
          {
          loader: 'css-loader',
          options: {
            sourceMap: true,
            modules: false,
            localIdentName: '[local][hash]'
          }
        }]
      },
      {
        test: /\.less/,
        use: ['style-loader', {
          loader: 'css-loader',
          options: {
            sourceMap: true,
            modules: false,
            localIdentName: '[name]__[local]__[hash]'
          }
        }, {
          loader: 'postcss-loader'
        }, {
          loader: 'less-loader',
          options: {
            outputStyle: 'expanded',
            javascriptEnabled: true
          }
        }],
      },
      {
        test: /\.(jpe?g|png|gif|svg)$/i,
        use: {
          loader: 'url-loader',
          options: {
            name: '[hash].[ext]',
            limit: 10000, // 10kb
          }
        }
      }
    ]
  },
  performance: {
    hints: false
  },
  plugins: [
    // new BundleAnalyzerPlugin(),
    new HappyPack({
      loaders: ['babel-loader']
    }),
    new webpack.DefinePlugin({
      'process.env': {
        NODE_ENV: JSON.stringify('production'),
        SC_ATTR: JSON.stringify('ocean-one-styled-component')
      }
    }),
    new webpack.NamedModulesPlugin(),
    new webpack.optimize.OccurrenceOrderPlugin(true),
    new MiniCssExtractPlugin({
      filename: '[name].[hash].css'
    }),
    new ManifestPlugin({
      fileName: 'mapping.json',
      publicPath: config.pathInMappingJson,
      seed: {
        title: config.title
      }
    })
  ]
};

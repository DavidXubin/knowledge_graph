const path = require('path');
const webpack = require('webpack');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const HappyPack = require('happypack');
const config = require('./config');

module.exports = {
  mode: 'development',
  entry: {
    app: ['react-hot-loader/patch', 'webpack-dev-server/client?http://0.0.0.0:3000', 'webpack/hot/only-dev-server', './src/index']
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src')
    },
    extensions: ['.js', '.jsx', '.less', '.png', '.jpg', '.gif']
  },
  devServer: {
    hot: true,
    contentBase: path.resolve(__dirname, 'public'),
    port: 3000,
    host: '0.0.0.0',
    publicPath: '/',
    historyApiFallback: {
      rewrites: [
        //多页面，则可以设置二级目录来区分
        { from: /^.*$/, to: `${config.context}/index.html` }
      ]
    },
    disableHostCheck: true,
    proxy: {
      '/server': {
        target: 'http://local.dev.bkjk-inc.com:8013',
        auth: false,
        changeOrigin: true
      }
    }
  },
  output: {
    path: path.join(__dirname, 'public'),
    publicPath: '/',
    filename: 'app.[hash].js'
  },
  devtool: 'source-map',
  module: {
    rules: [
      {
        test: /\.js(x?)$/,
        include: path.join(__dirname, 'src'),
        loader: 'babel-loader'
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
              localIdentName: '[local][hash:base64:5]'
            }
          }
        ]
      },
      {
        test: /\.less/,
        use: [
          'style-loader',
          {
            loader: 'css-loader',
            options: {
              sourceMap: true,
              modules: false,
              localIdentName: '[name]__[local]__[hash:base64:5]'
            }
          },
          {
            loader: 'postcss-loader'
          },
          {
            loader: 'less-loader',
            options: {
              outputStyle: 'expanded',
              javascriptEnabled: true
            }
          }
        ]
      },
      {
        test: /\.(jpe?g|png|gif|svg)$/i,
        use: {
          loader: 'url-loader',
          options: {
            name: '[hash].[ext]',
            limit: 10000 // 10kb
          }
        }
      }
    ]
  },
  performance: {
    hints: false
  },
  plugins: [
    new HappyPack({
      loaders: ['babel-loader']
    }),
    new MiniCssExtractPlugin({
      filename: '[name].css',
      chunkFilename: '[id].css'
    }),
    new webpack.DefinePlugin({
      'process.env': {
        SC_ATTR: JSON.stringify('ocean-one-styled-component') // 该配置时为了避免于其他使用styled-components的组件产生命名空间冲突
      }
    }),
    new webpack.NamedModulesPlugin(),
    new webpack.HotModuleReplacementPlugin(),
    new HtmlWebpackPlugin({ hash: false, template: './index.html', title: config.title }),
    new webpack.ContextReplacementPlugin(/moment[\/\\]locale$/, /nb/)
  ]
};

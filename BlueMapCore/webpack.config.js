const path = require('path')
const fs = require('fs')
const HtmlWebpackPlugin = require('html-webpack-plugin')
const MiniCssExtractPlugin = require('mini-css-extract-plugin');

const WEBROOT_PATH = path.resolve(__dirname, 'src/main/webroot')
const BUILD_PATH = path.resolve(__dirname, 'build/generated/webroot')
// folder with a generated world to render in the dev server
const WORLD_DATA_PATH = path.resolve(__dirname, 'build/generated/world')

module.exports = {
  mode: 'production',
  devtool: 'source-map',
  entry: {
    'bluemap': path.resolve(WEBROOT_PATH, 'js/site.js'),
  },
  output: {
    path: BUILD_PATH,
    filename: 'js/[name].js',
  },
  devServer: {
    contentBase: WORLD_DATA_PATH,
    compress: true,
    port: 8080,
    hot: true,
  },
  plugins: [
    new MiniCssExtractPlugin({
      filename: 'style/[name].css?[hash]',
    }),
    new HtmlWebpackPlugin({
      template: path.resolve(WEBROOT_PATH, 'index.html'),
      hash: true,
    }),
  ],
  resolve: {
    extensions: ['.js', '.css', '.scss'],
  },
  module: {
    rules: [
      // Just import normal css files
      {
        test: /\.css$/,
        include: /src/,
        use: [
          { loader: MiniCssExtractPlugin.loader },
          { loader: 'css-loader' },
        ],
      },
      // Converts scss files into css to use within custom elements
      {
        test: /\.scss$/,
        include: /src/,
        use: [
          { loader: MiniCssExtractPlugin.loader },
          { loader: 'css-loader' },
          { loader: 'sass-loader' },
        ],
      },
      // Load additional files
      {
        test: /\.(png|svg)(\?.*$|$)/,
        include: /src/,
        use: [
          {
            loader: 'file-loader',
            options: { name: 'assets/[name].[ext]?[hash]' },
          },
        ],
      },
    ],
  },
}

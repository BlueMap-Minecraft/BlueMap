import babel from "@rollup/plugin-babel";
import { terser } from "rollup-plugin-terser";

const babelrc = {
	babelHelpers: 'bundled',
	presets: [
		['@babel/preset-env', {
			targets: "> 0.25%, not dead",
			bugfixes: true,
			loose: true
		}]
	],
	plugins: [
		['@babel/plugin-proposal-class-properties', {
			loose: true
		}]
	]
};

export default [
	{
		input: 'src/BlueMap.js',
		external: [ 'three', 'hammerjs' ],
		plugins: [
			babel( {
				compact: false,
				babelrc: false,
				...babelrc
			} )
		],
		output: [
			{
				format: 'umd',
				name: 'BlueMap',
				file: 'build/bluemap.js',
				indent: '\t',
				globals: {
					three: 'THREE',
					hammerjs: 'Hammer',
				}
			}
		],
	},
	{
		input: 'src/BlueMap.js',
		external: [ 'three', 'hammerjs' ],
		plugins: [
			babel( {
				babelrc: false,
				...babelrc
			} ),
			terser(),
		],
		output: [
			{
				format: 'umd',
				name: 'BlueMap',
				file: 'build/bluemap.min.js',
				globals: {
					three: 'THREE',
					hammerjs: 'Hammer',
				}
			}
		]
	},
	{
		input: 'src/BlueMap.js',
		external: [ 'three', 'hammerjs' ],
		plugins: [],
		output: [
			{
				format: 'esm',
				file: 'build/bluemap.module.js'
			}
		]
	}
];
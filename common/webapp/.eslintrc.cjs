/* eslint-env node */
module.exports = {
  root: true,
  env: {
    es2022: true
  },
  'extends': [
    'plugin:vue/vue3-essential',
    'eslint:recommended'
  ],
  parserOptions: {
    ecmaVersion: 'latest'
  },
  rules: {
    "vue/multi-word-component-names": "off",
    "no-unused-vars": "off"
  }
}

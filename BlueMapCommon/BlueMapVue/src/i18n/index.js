import VueI18n from 'vue-i18n';
import Vue from 'vue';
import fallback from './fallback';

VueI18n.prototype.setLanguage = async function(lang) {
	try {
		if (!this.messages[lang]){
			let messages = {};
			messages = (await import(/* webpackIgnore: true */ `../lang/${lang}.js`)).default;

			this.setLocaleMessage(lang, messages);
		}

		this.locale = lang;
		document.querySelector('html').setAttribute('lang', lang);
	} catch (e) {
		console.error(`Failed to load language '${lang}'!`, e);
	}
}

VueI18n.prototype.loadLanguageSettings = async function() {
	let settings = (await import(/* webpackIgnore: true */ "../lang/settings.js")).default;
	this.languages = settings.languages;
	this.setLanguage(settings.default);
};

Vue.use(VueI18n);

const i18n = new VueI18n({
	locale: 'fallback',
	fallbackLocale: 'fallback',
	silentFallbackWarn: true,
	messages: { fallback }
});

export default i18n;

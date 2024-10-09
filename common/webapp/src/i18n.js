import {createI18n} from 'vue-i18n';
import {nextTick} from "vue";
import {fetchHocon} from "./js/Utils";

export const i18nModule = createI18n({
	locale: 'none',
	fallbackLocale: 'en',
	silentFallbackWarn: true,
	warnHtmlMessage: false,
	legacy: false,
	messages: {}
});

export const i18n = i18nModule.global;

export async function setLanguage(lang) {
	try {
		if (!i18n.availableLocales.includes(lang)){
			let messages = await fetchHocon(`./lang/${lang}.conf`);
			i18n.setLocaleMessage(lang, messages);
		}

		i18n.locale.value = lang;

		document.querySelector('html').setAttribute('lang', lang);
	} catch (e) {
		console.error(`Failed to load language '${lang}'!`, e);
	}

	return nextTick();
}

export async function loadLanguageSettings() {
	let settings = await fetchHocon(`./lang/settings.conf`);
	let selectedLanguage = null;

	if (settings.useBrowserLanguage) {
		const availableLanguages = settings.languages.map(lang => lang.locale);

        for (let browserLanguage of navigator.languages) {
            selectedLanguage = availableLanguages.find(lang => lang === browserLanguage);
            if (selectedLanguage) break;

            let baseBrowserLanguage = browserLanguage.split('-')[0];
            selectedLanguage = availableLanguages.find(lang => lang.startsWith(baseBrowserLanguage));
            if (selectedLanguage) break;
        }
	}

	if (!selectedLanguage) {
		selectedLanguage = settings.default;
	}

	i18n.languages = settings.languages;
	await setLanguage(selectedLanguage);
}

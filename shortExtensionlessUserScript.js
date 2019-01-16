// This is a version of the userscript that does not require an extension like GreaseMonkey
// You'll need to manually inject the code instead, your data (such as lurk type and passwords)
//   are essentially stored in browser cookies instead of in the extension's storage
// How to use this userscript:
// access the developer's console (this is usually opened by pressing F12 or looking in your browser's tools)
//   if you're having trouble with this, googling developer's console or developer's tools may help
// then paste this code into the console (the text area marked with a ">")
// press enter to run the userscript

var       GM_info = "info",
    userscriptURL = "https://glcdn.githack.com/jtrygva/trollegle/raw/master/trollegle.user.js";

var script = document.createElement("script");
script.src = "https://greasemonkey.github.io/gm4-polyfill/gm4-polyfill.js"; 
document.body.appendChild(script);

script.onload = function() {
  var ls = localStorage;
  GM.getValue = ls.getItem.bind(ls);
  GM.setValue = ls.setItem.bind(ls);
  
  script = document.createElement("script");
  script.src = userscriptURL;
  document.body.appendChild(script);
};
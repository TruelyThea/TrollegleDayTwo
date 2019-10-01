// This is a version of the userscript that does not require an extension like GreaseMonkey
// You'll need to manually inject the code instead, your data (such as lurk type and passwords)
//   are essentially stored in browser cookies instead of in the extension's storage
// How to use this userscript:
// access the developer's console (this is usually opened by pressing F12 or looking in your browser's tools)
//   if you're having trouble with this, googling developer's console or developer's tools may help
// then paste this code into the console (the text area marked with a ">")
// press enter to run the userscript

var GM_info = "info";

var script = document.createElement("script");
script.src = "https://greasemonkey.github.io/gm4-polyfill/gm4-polyfill.js";
document.body.appendChild(script);

script.onload = function() {
  var ls = localStorage;
  GM.getValue = ls.getItem.bind(ls);
  GM.setValue = ls.setItem.bind(ls);
  
  userscript();
};


function userscript() {
  // ==UserScript==
  // @name Trollegle
  // @version 0.0.8
  // @match *://*.omegle.com/
  // @grant GM_getValue
  // @grant GM_setValue
  // @grant GM.getValue
  // @grant GM.setValue
  // @grant unsafeWindow
  // @require https://greasemonkey.github.io/gm4-polyfill/gm4-polyfill.js
  // @copyright SHA-1 module used under licence from Chris Veness, rest is public domain
  // @updateURL https://bellawhiskey.ca/trollegle.user.js
  // ==/UserScript==

  var rooms = [];
  var state = {
    window: null, 
    captchaWidget: null, 
    captchaKey: null, 
    captchas: 0, 
    backend: null, 
    connectTime: null,
    isTrollegle: false,
    lurkMode: null,
    lurkRate: null,
    lastLurkRate: null,
    lurkText: null,
    lurkHandle: null,
  };

  var lurkModes = [
    {name: "never", value: "never"},
    {name: "when a room is detected", value: "rooms"},
    {name: "always", value: "always"},
  ];

  async function lurkMode(def) { return state.lurkMode || await GM.getValue("lurkMode") || "never" }
  async function lurkRate(def) { return state.lurkRate || Number(await GM.getValue("lurkRate")) || 10 }
  async function lurkText(def) { return state.lurkText || await GM.getValue("lurkText") || "/8|/id|/list" }

  var sty = E("style", {}, "#prefbox>:not(button) {" +
    "display: block; font-family: inherit; font-size: inherit; " +
    "-moz-appearance: none; -webkit-appearance: none; margin: 0.2em; }" +
    "#prefbox input { vertical-align: middle; }" +
    "#prefbox textarea { border: solid black 1px; width: 95%; height: 10em; }" +
    ".sublabel { display:inline-block; margin-left: 1em; width: 5.5em; }" +
    "#prefbox * { font-size: inherit; }");
  document.head.appendChild(sty);

  async function updateRooms() {
    rooms = parseRooms(await GM.getValue("rooms") || "");
  }

  function parseRooms(text) {
    return text.split("\n")
        .map((a) => a.split(" "))
        .filter((a) => a.length == 3);
  }

  async function saveRooms(text) {
    await GM.setValue("rooms", text);
  }

  function inspectPost(text) {
    let parts = text.split(/ +/);
    if (parts.length >= 5 && parts[1] == "Login" && parts[2] == "challenge:")
      return {action: async () => !await GM.getValue("disabled")
              && inspectChallenge(parts[3], parts[4])};
    
    if (text.match(/^\s*\| .*is +your +username/))
      return {action: async () => {
        state.isTrollegle = true;
        await GM.getValue("autoCaptchaEnabled") && say("/cap");
        await GM.getValue("colorEnabled") && coloredMain();
        updateLurk();
      }};
    
    if (text.match(/^\s*#A /)) {
      try {
        let obj = JSON.parse(text.slice(text.indexOf("{")));
        if ("captchas" in obj)
          return {action: async () => !await GM.getValue("captchaDisabled")
                  && inspectCaptcha(obj),
                  override: logCaptchaCount(obj.captchas)};
      } catch (e) {
        console.log("Bad JSON: " + text);
      }
    }
    return {};
  }

  function logCaptchaCount(count) {
    if (count == state.captchas)
      return events => events.push(["stoppedTyping"]);
    state.captchas = count;
    return events => {
      events.push(["stoppedTyping"]);
      events.push(["serverMessage", "Captchas to solve: " + count]);
    };
  }

  function inspectChallenge(salt, challenge) {
    var matching = rooms.filter((a) => Sha1.hash(salt + a[1]) == challenge);
    if (matching.length) {
      if (matching.length == 1) {
        var a = matching[0];
        log("This *seems* to be " + a[0]);
        say("/password " + Sha1.hash(salt + a[2]));
      } else {
        log("More than one matching room found (" + matching.map((a) => a[0]).join(", ") +
            "). There might be something amiss in the settings.");
      }
    } else {
      log("This doesn't look like a known room");
    }
  }

  function say(text) {
    var chatmsg = document.querySelector(".chatmsg");
    chatmsg.value = text;
    var sendbtn = document.querySelector(".sendbtn");
    if (sendbtn)
      sendbtn.click();
    else
      chatmsg.parentNode.dispatchEvent(new MouseEvent("submit"));
  }

  function sayQuiet(text, description) {
    if (state.backend) {
      console.log("sayQuiet: " + text);
      unXray(state.backend).sendMessage(text);
      log(description);
    } else {
      say(text);
    }
  }

  function clearSession() {
    clearCaptcha();
    state.connectTime = null;
    state.isTrollegle = false;
    clearLurk();
  }

  async function shouldLurk() {
    let mode = await lurkMode();
    return mode == "always" && state.connectTime || mode == "rooms" && state.isTrollegle;
  }

  async function updateLurk() {
    let enabled = await shouldLurk();
    let newRate = await lurkRate();
    if (state.lurkHandle && enabled && newRate != state.lastLurkRate) {
      clearInterval(state.lurkHandle);
      setLurk(newRate);
    } else if (state.lurkHandle && !enabled) {
      clearLurk();
    } else if (!state.lurkHandle && enabled) {
      setLurk(newRate);
    }
    state.lastLurkRate = newRate;
  }

  function setLurk(rate) {
    log("Setting lurk timer for " + rate + " min");
    if (Date.now() - state.connectTime > 1000 * 60)
      lurk();
    state.lurkHandle = setInterval(lurk, 1000 * 60 * rate);
  }

  async function lurk() {
    if (document.querySelector(".chatmsg").value) return;
    let textSpec = await lurkText();
    let choices = textSpec.split(/\|/);
    let choice = choices[Math.floor(Math.random() * choices.length)];
    say(choice.replace("[TIMESTAMP]", Math.floor(Date.now() / 1000)));
  }

  function clearLurk() {
    if (state.lurkHandle)
      log("Stopping lurk timer");
    clearInterval(state.lurkHandle);
    state.lurkHandle = null;
  }

  function inspectCaptcha(obj) {
    if (obj.key) {
      state.captchaKey = obj.key;
      console.log("set key: " + state.captchaKey);
    }
    if (obj.captchas)
      showCaptcha();
    else if (obj.captchas === 0)
      hideCaptcha();
    console.log("key after inspectCaptcha: " + state.captchaKey);
  }

  function clearCaptcha() {
    state.captchas = 0;
    hideCaptcha();
  }

  function showCaptcha() {
    try {
      let container = ensureCaptchaElement();
      if (state.captchaWidget === null || !container.children.length) {
        state.captchaWidget = state.window.grecaptcha.render(clone(container), clone({
          sitekey: state.captchaKey,
          callback: wrap(submitCaptcha),
        }));
      }
      container.style.visibility = "visible";
    } catch (e) {
      console.log(e);
    }
  }

  function hideCaptcha() {
    let container = ensureCaptchaElement();
    container.style.visibility = "collapse";
    resetCaptcha();
  }

  function ensureCaptchaElement() {
    let element = document.querySelector("#trollegle-captcha");
    if (!element) {
      document.body.appendChild(
        element = E("div", {id: "trollegle-captcha"}));
      style(element, {position: "absolute", top: "0", left: "0"});
    }
    return element;
  }

  function submitCaptcha(response) {
    sayQuiet("/cap " + JSON.stringify({key: state.captchaKey, token: response}), 
            "Captcha submitted");
    resetCaptcha();
  }

  function resetCaptcha() {
    let container = ensureCaptchaElement();
    if (state.captchaWidget !== null && container.children.length)
      state.window.grecaptcha.reset(state.captchaWidget);
  }

  function log(message) {
    document.querySelector(".logbox").children[0].appendChild(
        E("div", {"class": "logitem"}, 
          E("p", {"class": "statuslog"}, message)));
  }

  function E(tag, attrs, content) {
    function addContent(elem, content) {
      if (content instanceof Node) {
        elem.appendChild(content);
      } else {
        if (elem.styleSheet) {
          elem.styleSheet.cssText = (elem.styleSheet.cssText || "") + content;
        } else {
          elem.appendChild(document.createTextNode(content));
        }
      }
    }
    var elem = document.createElement(tag);
    if (attrs) Object.getOwnPropertyNames(attrs).forEach(function(name) {
      elem.setAttribute(name, attrs[name]);
    });
    if (content) {
      if (content instanceof Array)
        content.forEach(function(a) {addContent(elem, a)});
      else
        addContent(elem, content);
    }
    return elem;
  };

  function style(elem, spec) {
    Object.getOwnPropertyNames(spec).forEach(function(prop) {
      elem.style[prop] = spec[prop];
    });
    return elem;
  }

  async function editor(callback) {
    var win, list, lurkModeList, lurkRateInput, lurkTextInput, 
      captchaCheck, autoCaptchaCheck, colorCheck, loginCheck, save, cancel;
    win = E("form", {id: "prefbox"}, [
      E("div", {}, [
        E("label", {"for": "lurkMode"}, "Lurk: "),
        lurkModeList = E("select", {id: "lurkMode"},
          lurkModes.map(mode => E("option", {"value": mode.value}, mode.name))),
      ]),
      E("div", {}, [
        E("label", {"for": "lurkText", "class": "sublabel"}, "sending "),
        lurkTextInput = E("input", {id: "lurkText"}),
      ]),
      E("div", {}, [
        E("label", {"for": "lurkRate", "class": "sublabel"}, "every "),
        lurkRateInput = E("input", {id: "lurkRate", type: "number"}),
        E("label", {"for": "lurkRate"}, " min"),
      ]),
      E("div", {}, [
        captchaCheck = E("input", {id: "captchaEnabled", type: "checkbox"}),
        E("label", {"for": "captchaEnabled"}, "Load captcha widget"),
      ]),
      E("div", {}, [
        autoCaptchaCheck = E("input", {id: "autoCaptcha", type: "checkbox"}),
        E("label", {"for": "autoCaptcha"}, "Ask room for captchas upon joining"),
      ]),
      E("div", {}, [
	colorCheck = E("input", {id: "color", type: "checkbox"}),
	E("label", {"for": "color"}, "Colorize room"),
      ]),
      E("div", {}, [
        loginCheck = E("input", {id: "enabled", type: "checkbox"}),
        E("label", {"for": "enabled"}, "Enable admin login with credentials:"),
      ]),
      list = E("textarea", {id: "rooms"}, await GM.getValue("rooms")),
      E("label", {"for": "rooms"}, ["(one room per line, ",
                    E("em", {}, "roomname challenge response"), ")"]),
      save = E("button", {type: "button"}, "Save"),
      cancel = E("button", {type: "button"}, "Cancel"),
    ]);
    [list, lurkModeList, lurkRateInput, lurkTextInput, captchaCheck, autoCaptchaCheck, loginCheck, save, cancel]
      .forEach(widget => widget.onkeyup = widget.onkeydown = ev => ev.stopPropagation());
    lurkModeList.value = await lurkMode();
    lurkRateInput.value = await lurkRate();
    lurkTextInput.value = await lurkText();
    colorCheck.checked = await GM.getValue("colorEnabled");
    captchaCheck.checked = !await GM.getValue("captchaDisabled");
    autoCaptchaCheck.checked = await GM.getValue("autoCaptchaEnabled");
    loginCheck.checked = !await GM.getValue("disabled");
    style(win, {
      display: "block",
      position: "fixed",
      right: 0,
      top: 0,
      border: "solid black 0.1em",
      background: "white",
      color: "black",
      zIndex: 100,
      padding: "0.2em",
    });
    style(lurkRateInput, {width: "4em"});
    style(lurkTextInput, {width: "11em"});
    save.onclick = async function() {
      if (!captchaCheck.checked)
        hideCaptcha();
      else if (state.captchas)
        showCaptcha();
      GM.setValue("captchaDisabled", !captchaCheck.checked && "true" || "");
      GM.setValue("autoCaptchaEnabled", autoCaptchaCheck.checked && "true" || "");
      GM.setValue("colorEnabled", colorCheck.checked && "true" || "");
      GM.setValue("disabled", !loginCheck.checked && "true" || "");
      GM.setValue("lurkText", state.lurkText = lurkTextInput.value);
      await GM.setValue("lurkRate", state.lurkRate = Number(lurkRateInput.value));
      await GM.setValue("lurkMode", state.lurkMode = lurkModeList.value);
      await saveRooms(list.value);
      win.parentNode.removeChild(win);
      callback();
      return false;
    };
    cancel.onclick = function() {
      win.parentNode.removeChild(win);
      callback();
      return false;
    };
    document.body.appendChild(win);
  }

  function win() {
    try {
      return unsafeWindow;
    } catch (e) {
      return window;
    }
  }

  function unXray(obj) {
    return obj.wrappedJSObject || obj;
  }

  function wrap(fun) {
    try {
      return exportFunction(fun, unsafeWindow, {allowCrossOriginArguments: true});
    } catch (e) {
      return fun;
    }
  }

  function clone(obj) {
    try {
      return cloneInto(obj, unsafeWindow, {cloneFunctions: true, wrapReflectors: true});
    } catch (e) {
      return obj;
    }
  }

  function begin() {
    let window = state.window = win();
    if (!window.COMETBackend) {
      setTimeout(begin, 500);
    } else {
      if (!window.COMETBackend.prototype.Trollegle$gotEvents) {
        window.COMETBackend.prototype.Trollegle$gotEvents = 
          window.COMETBackend.prototype.gotEvents;
        window.COMETBackend.prototype.Trollegle$disconnect = 
          window.COMETBackend.prototype.disconnect;
        var button = document.createElement("button");
        var update = async function() {
          button.textContent = "Trollegle ▾";
          updateRooms();
          updateLurk();
        };
        window.addEventListener("focus", update);
        update();
        button.onclick = function() {
          editor(update);
          return false;
        };
        (document.querySelector("#sharebuttons")
          || document.querySelector("#header")).appendChild(button);
      }
      var newGotEvents = function (events) {
        state.backend = this;
        if (!state.connectTime) {
          state.connectTime = Date.now();
          updateLurk();
        }
        let remaining = [];
        events.forEach(function (event) {
          if (event[0] == "gotMessage") {
            let result = inspectPost(event[1]);
            if (result.action)
              result.action();
            if (result.override) {
              console.log("gotMessage overridden: " + event[1]);
              result.override(remaining);
              return;
            }
          }
          remaining.push(event);
        });
        unXray(this).Trollegle$gotEvents(clone(remaining));
        if (unXray(this).stopped)
          clearSession();
      };
      var newDisconnect = function () {
        unXray(this).Trollegle$disconnect();
        clearSession();
      };
      window.COMETBackend.prototype.gotEvents = wrap(newGotEvents);
      window.COMETBackend.prototype.disconnect = wrap(newDisconnect);
    }
  }

// Code for colorizing
// new message listener and color assignment
function coloredMain() {
    'use strict';
    say("/duids");
    var msges = document.getElementsByClassName('logbox')[0].firstChild;
    var background = document.getElementsByClassName('logbox')[0];
    background.style.background = `#1e394c`;
    background.style.fontFamily = `monospace`;
    background.style.fontSize = "13pt";
    var idRegex = /.*?\((\d+)\)/gm;

    function change(obj){
        var idstr = idRegex.exec(obj.innerHTML);
        if (idstr != null){
            var col = parseInt(idstr[1]) * 20;
            obj.style.color = `hsl(${col},100%,60%)`;
        }else{
            obj.style.color = `#ffffff`;
        }
    }

    var observrOptions = {
        childList: true
    }

    var observr = new MutationObserver(mutationList => {
        // Loop over the mutations
        mutationList.forEach(mutation => {
            // For added nodes apply the color function
            mutation.addedNodes.forEach(node => {
                change(node)
                //console.log(node)
            })
        })
    })

    observr.observe(msges, observrOptions);
}


  begin();

  /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -  */
  /* SHA-1 (FIPS 180-4) implementation in JavaScript                    (c) Chris Veness 2002-2016  */
  /*                                                                                   MIT Licence  */
  /* www.movable-type.co.uk/scripts/sha1.html                                                       */
  /*                                                                                                */
  /*  - see http://csrc.nist.gov/groups/ST/toolkit/secure_hashing.html                              */
  /*        http://csrc.nist.gov/groups/ST/toolkit/examples.html                                    */
  /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -  */



  /**
   * SHA-1 hash function reference implementation.
   *
   * This is a direct implementation of FIPS 180-4, without any optimisations. It is intended to aid
   * understanding of the algorithm rather than for production use, though it could be used where
   * performance is not critical.
   *
   * @namespace
   */
  var Sha1 = {};

  (function () {
  'use strict';


  /**
   * Generates SHA-1 hash of string.
   *
   * @param   {string} msg - (Unicode) string to be hashed.
   * @param   {Object} [options]
   * @param   {string} [options.msgFormat=string] - Message format: 'string' for JavaScript string
   *   (gets converted to UTF-8 for hashing); 'hex-bytes' for string of hex bytes ('616263' ≡ 'abc') .
   * @param   {string} [options.outFormat=hex] - Output format: 'hex' for string of contiguous
   *   hex bytes; 'hex-w' for grouping hex bytes into groups of (4 byte / 8 character) words.
   * @returns {string} Hash of msg as hex character string.
   */
  Sha1.hash = function(msg, options) {
      var defaults = { msgFormat: 'string', outFormat: 'hex' };
      var opt = Object.assign(defaults, options);

      switch (opt.msgFormat) {
          default: // default is to convert string to UTF-8, as SHA only deals with byte-streams
          case 'string':   msg = Sha1.utf8Encode(msg);       break;
          case 'hex-bytes':msg = Sha1.hexBytesToString(msg); break; // mostly for running tests
      }

      // constants [§4.2.1]
      var K = [ 0x5a827999, 0x6ed9eba1, 0x8f1bbcdc, 0xca62c1d6 ];

      // initial hash value [§5.3.1]
      var H = [ 0x67452301, 0xefcdab89, 0x98badcfe, 0x10325476, 0xc3d2e1f0 ];

      // PREPROCESSING [§6.1.1]

      msg += String.fromCharCode(0x80);  // add trailing '1' bit (+ 0's padding) to string [§5.1.1]

      // convert string msg into 512-bit/16-integer blocks arrays of ints [§5.2.1]
      var l = msg.length/4 + 2; // length (in 32-bit integers) of msg + ‘1’ + appended length
      var N = Math.ceil(l/16);  // number of 16-integer-blocks required to hold 'l' ints
      var M = new Array(N);

      for (var i=0; i<N; i++) {
          M[i] = new Array(16);
          for (var j=0; j<16; j++) {  // encode 4 chars per integer, big-endian encoding
              M[i][j] = (msg.charCodeAt(i*64+j*4)<<24) | (msg.charCodeAt(i*64+j*4+1)<<16) |
                  (msg.charCodeAt(i*64+j*4+2)<<8) | (msg.charCodeAt(i*64+j*4+3));
          } // note running off the end of msg is ok 'cos bitwise ops on NaN return 0
      }
      // add length (in bits) into final pair of 32-bit integers (big-endian) [§5.1.1]
      // note: most significant word would be (len-1)*8 >>> 32, but since JS converts
      // bitwise-op args to 32 bits, we need to simulate this by arithmetic operators
      M[N-1][14] = ((msg.length-1)*8) / Math.pow(2, 32); M[N-1][14] = Math.floor(M[N-1][14]);
      M[N-1][15] = ((msg.length-1)*8) & 0xffffffff;

      // HASH COMPUTATION [§6.1.2]

      for (var i=0; i<N; i++) {
          var W = new Array(80);

          // 1 - prepare message schedule 'W'
          for (var t=0;  t<16; t++) W[t] = M[i][t];
          for (var t=16; t<80; t++) W[t] = Sha1.ROTL(W[t-3] ^ W[t-8] ^ W[t-14] ^ W[t-16], 1);

          // 2 - initialise five working variables a, b, c, d, e with previous hash value
          var a = H[0], b = H[1], c = H[2], d = H[3], e = H[4];

          // 3 - main loop (use JavaScript '>>> 0' to emulate UInt32 variables)
          for (var t=0; t<80; t++) {
              var s = Math.floor(t/20); // seq for blocks of 'f' functions and 'K' constants
              var T = (Sha1.ROTL(a,5) + Sha1.f(s,b,c,d) + e + K[s] + W[t]) >>> 0;
              e = d;
              d = c;
              c = Sha1.ROTL(b, 30) >>> 0;
              b = a;
              a = T;
          }

          // 4 - compute the new intermediate hash value (note 'addition modulo 2^32' – JavaScript
          // '>>> 0' coerces to unsigned UInt32 which achieves modulo 2^32 addition)
          H[0] = (H[0]+a) >>> 0;
          H[1] = (H[1]+b) >>> 0;
          H[2] = (H[2]+c) >>> 0;
          H[3] = (H[3]+d) >>> 0;
          H[4] = (H[4]+e) >>> 0;
      }

      // convert H0..H4 to hex strings (with leading zeros)
      for (var h=0; h<H.length; h++) H[h] = ('00000000'+H[h].toString(16)).slice(-8);

      // concatenate H0..H4, with separator if required
      var separator = opt.outFormat=='hex-w' ? ' ' : '';

      return H.join(separator);
  };


  /**
   * Function 'f' [§4.1.1].
   * @private
   */
  Sha1.f = function(s, x, y, z)  {
      switch (s) {
          case 0: return (x & y) ^ (~x & z);           // Ch()
          case 1: return  x ^ y  ^  z;                 // Parity()
          case 2: return (x & y) ^ (x & z) ^ (y & z);  // Maj()
          case 3: return  x ^ y  ^  z;                 // Parity()
      }
  };

  /**
   * Rotates left (circular left shift) value x by n positions [§3.2.5].
   * @private
   */
  Sha1.ROTL = function(x, n) {
      return (x<<n) | (x>>>(32-n));
  };


  /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -  */


  /**
   * Encodes multi-byte string to utf8 - monsur.hossa.in/2012/07/20/utf-8-in-javascript.html
   */
  Sha1.utf8Encode = function(str) {
      return unescape(encodeURIComponent(str));
  };


  /**
   * Converts a string of a sequence of hex numbers to a string of characters (eg '616263' => 'abc').
   */
  Sha1.hexBytesToString = function(hexStr) {
      hexStr = hexStr.replace(' ', ''); // allow space-separated groups
      var str = '';
      for (var i=0; i<hexStr.length; i+=2) {
          str += String.fromCharCode(parseInt(hexStr.slice(i, i+2), 16));
      }
      return str;
  };
  })();
};

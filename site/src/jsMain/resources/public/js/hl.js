// Registers a small ProGuard / R8 keep-rule grammar with highlight.js (used by the R8 blog post).
(function () {
  function register() {
    if (!window.hljs) return setTimeout(register, 50);
    window.hljs.registerLanguage('proguard', function (hljs) {
      return {
        name: 'ProGuard',
        aliases: ['pro', 'r8'],
        contains: [
          hljs.HASH_COMMENT_MODE,
          { className: 'keyword', begin: /-[a-z][a-zA-Z]+/ },
          { className: 'type', begin: /\b(class|interface|enum|extends|implements|public|protected|private|static|final|native|abstract)\b/ },
          { className: 'meta', begin: /@[\w.]+/ },
          { className: 'variable', begin: /<(fields|methods|init|clinit|\d+)>/ },
          { className: 'string', begin: /\*\*|\*/ },
          { className: 'title', begin: /\b[a-z][\w]*(\.[\w$]+)+/ },
        ],
      };
    });
  }
  register();
})();

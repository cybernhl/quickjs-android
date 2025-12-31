var Clock = (function () {

  let now = 0;

  function tick(t) {
    now = t;
  }

  function millis() {
    return now;
  }

  return {
    tick,
    millis
  };
})();

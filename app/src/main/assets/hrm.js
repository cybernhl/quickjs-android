/*
  舊的程式碼：每次呼叫 next() 都會更新 bpm
  let bpm=72;
  let dir=1;

  function next(){
    bpm+=dir*(Math.random()*2);
    if(bpm>85) dir=-1;
    if(bpm<65) dir=1;
    return Math.round(bpm);
  }
*/

// --- 新的程式碼 ---

//// 全域變數，用於儲存心率值、方向和上次更新的時間戳
//var bpm = 72;
//var dir = 1;
//var lastUpdateTime = 0;
//
//// Date.now() 返回自 1970 年 1 月 1 日 00:00:00 UTC 以來的毫秒數
//function now_ms() {
//  return Date.now();
//}
//
//// 核心函式，現在包含了計時邏輯
//function next() {
//  const currentTime = now_ms();
//
//  if (currentTime - lastUpdateTime > 15000) {
//    bpm += dir * (Math.random() * 2);
//    if (bpm > 85) dir = -1;
//    if (bpm < 65) dir = 1;
//    lastUpdateTime = currentTime;
//
//    console.log('[hrm.js] BPM was updated to: ' + Math.round(bpm));
//  }
//
//  // 為了保持 evaluate("next()") 能工作，我們仍然回傳 bpm
//  return Math.round(bpm);
//}

registerGatt("2A37", (function () {

  let bpm = 72;
  let dir = 1;
  let last = 0;

  function next() {
    const now = Clock.millis();
    if (last === 0) {
      last = now;
      return;
    }

    if (now - last >= 1000) {
      bpm += dir * (Math.random() * 2);
      if (bpm > 85) dir = -1;
      if (bpm < 65) dir = 1;
      last = now;
    }
  }

  function getValue() {
    return Math.round(bpm);
  }

  return {
    next,
    getValue
  };
})());
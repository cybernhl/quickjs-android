/**
 * 模擬電池電量 (Battery Level) 的變化
 */

//// 使用 'var' 宣告為全域變數，以便 Java/Kotlin 層可以透過 globalObject 訪問
//var batteryLevel = 100;
//
//// 上次電量變化的時間戳
//var lastBatteryUpdateTime = 0;
//
//// 模擬電量下降
//function decreaseBattery() {
//  const currentTime = Date.now(); // 使用內建的 Date.now()
//
//  // 每 3 秒 (3000 毫秒) 更新一次電量
//  if (currentTime - lastBatteryUpdateTime > 3000) {
//    // 隨機減少 1% 或 2%
//    batteryLevel -= Math.floor(Math.random() * 2) + 1;
//
//    // 確保電量不會低於 20%
//    if (batteryLevel < 20) {
//      batteryLevel = 100; // 如果電量過低，重新充滿以循環模擬
//    }
//
//    lastBatteryUpdateTime = currentTime;
//    console.log('[battery.js] Battery level was updated to: ' + batteryLevel);
//  }
//
//  // 即使不更新，也回傳目前的值
//  return batteryLevel;
//}
registerGatt("2A19", (function () {

  let level = 100;
  let last = 0;

  function next() {
    const now = Clock.millis();
    if (now - last >= 5000) { // 每 5 秒掉一點
      level -= Math.random() * 0.2;
      if (level < 0) level = 100;
      last = now;
    }
  }

  function getValue() {
    return Math.round(level);
  }

  return {
    next,
    getValue
  };
})());
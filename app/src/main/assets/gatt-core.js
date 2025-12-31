// 全域 registry（一開始是空的）
const __gattRegistry = {};

// 提供給各 js module 註冊用
function registerGatt(uuid, impl) {
  if (__gattRegistry[uuid]) {
    console.log("Gatt already registered:", uuid);
  }
  __gattRegistry[uuid] = impl;
}

// 統一對外 API（Java 只會用這兩個）
function next(uuid) {
  const m = __gattRegistry[uuid];
  if (!m || !m.next) {
    console.log("next(): no gatt for", uuid);
    return;
  }
  m.next();
}

function getValue(uuid) {
  const m = __gattRegistry[uuid];
  if (!m || !m.getValue) {
    console.log("getValue(): no gatt for", uuid);
    return -1;
  }
  return m.getValue();
}


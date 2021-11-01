function GraphDictionary() {
    this.data = new Array();

    this.add = add;
    this.find = find;
    this.remove = remove;
    this.showAll = showAll;
    this.getKeys = getKeys;
    this.count = count;
    this.clear = clear;
    this.contains = contains;
}

function add(key, value) {
    this.data[key] = value;
}

function find(key) {
    return this.data[key];
}

function contains(key) {
    const keys = this.getKeys();
    return keys.includes(key)
}

function remove(key) {
    delete this.data[key];
}

function showAll() {
    // 使用Object.keys获取所有, sort排序
    for (var key in Object.keys(this.data).sort()) {
        print(key + " -> " + this.data[k])
    }
}

function getKeys() {
    return Object.keys(this.data)
}

function count() {
    return this.data.length;
}

function clear() {
    for (var key in Object.keys(this.data)) {
        delete this.data[key];
    }
}


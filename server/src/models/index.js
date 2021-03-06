import mongoose from 'mongoose';
import bulk from 'bulk-require';
import config from '../config';

const dbConfig = config.db;
mongoose.connect(`${dbConfig.username}:${dbConfig.password}@${dbConfig.host}:${dbConfig.port}/${dbConfig.database}`);

const models = bulk(__dirname, ['./!(*index).js']); // 路由配置文件的集合, 忽略 index.js
const db = {};

// 加载 model
Object.keys(models).forEach((item) => {
  db[item] = mongoose.model(item, new mongoose.Schema(models[item].default));
});

export default db;

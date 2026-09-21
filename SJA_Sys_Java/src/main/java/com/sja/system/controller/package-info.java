/**
 * HTTP 接口适配层，按认证、稿费、人员、系统等业务域分包。
 *
 * <p>Controller 只负责参数绑定、方法鉴权和响应转换，不得直接访问 DAL 或编排业务流程。</p>
 *
 * 202609211427 - 完成了Controller按业务域整理。
 */
package com.sja.system.controller;

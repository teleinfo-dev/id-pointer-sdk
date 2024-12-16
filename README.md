# id-pointer-sdk

简体中文 | [English](./README_EN.md)

----

## 介绍

id-pointer-sdk 是基于Java语言开发的项目，是工业互联网标识解析体系的客户端软件开发工具包，主要提供对工业互联网标识服务的标识解析，标识管理等功能。


## 状态

v2.3.1 released


## 下载

通过Maven下载最新的jar

```xml
<dependency>
  <groupId>cn.teleinfo.id-pointer</groupId>
  <artifactId>id-pointer-sdk</artifactId>
  <version>2.2.3</version>
</dependency>
```

## 快速开始

## 版本设置

```
mvn versions:set -DgenerateBackupPoms=false -DnewVersion=2.3.1 && mvn -N versions:update-child-modules
mvn versions:set -DgenerateBackupPoms=false -DnewVersion=2.3.2-SNAPSHOT && mvn -N versions:update-child-modules

```

## 维护人员

[teleinfo](https://www.teleinfo.cn)

## 使用案例

## 贡献者

欢迎对项目进行贡献!请先阅读[issue指南](./doc/ContributorCovenant.md)。

## 特别鸣谢

此项目是基于工业互联网与物联网研究所研发的[ID-SDK](https://github.com/4iot-dev/ID-SDK)进行迭代开发，项目最初是从ID-SDK项目fork创建，感谢工业互联网与物联网研究所在开源项目上的贡献。

## Licence

```
Copyright 2020 Teleinfo, Co.Ltd.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

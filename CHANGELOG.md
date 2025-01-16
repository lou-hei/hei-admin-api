# [1.90.0](https://github.com/hei-school/hei-admin-api/compare/v1.89.0...v1.90.0) (2025-01-16)


### Features

* **not-implemented:** attribute an event with a color ([d740ffa](https://github.com/hei-school/hei-admin-api/commit/d740ffa5d0a9eabdac8f5c6969aaf42d84ba23df))



# [1.89.0](https://github.com/hei-school/hei-admin-api/compare/v1.88.0...v1.89.0) (2025-01-15)


### Bug Fixes

* certificate studying year ([516c709](https://github.com/hei-school/hei-admin-api/commit/516c70969878f616dc574e200bd6879b3a2ffc55))
* crupdateAndLinkMonitor sql exception ([ac8f1a6](https://github.com/hei-school/hei-admin-api/commit/ac8f1a67d64ad72a70b987ab13835d8bb2176501))


### Features

* add color code for event attribute  ([0ee30c2](https://github.com/hei-school/hei-admin-api/commit/0ee30c26c637a60943576af2afaf992aae5653f8))
* attaching monitor to multiple students ([b476b8a](https://github.com/hei-school/hei-admin-api/commit/b476b8a95aa2d403c778fe247b7f32bfe7a3ef90))
* zip multiple feeReceipts ([3829189](https://github.com/hei-school/hei-admin-api/commit/3829189d41c5b57c7aaf86a29211c051ed741e34))



# [1.88.0](https://github.com/hei-school/hei-admin-api/compare/v1.87.0...v1.88.0) (2025-01-08)


### Bug Fixes

* verify mpbs via xls ([0c4b607](https://github.com/hei-school/hei-admin-api/commit/0c4b607ef24345f3bb9b37f635ad419e59b27d86))


### Features

* GET /staff_members/xlsx  ([f8339c3](https://github.com/hei-school/hei-admin-api/commit/f8339c3af319ee902436b010c402f2a280e50f68))


### Reverts

* Revert "feat: filter fees by status and type " ([f2b8345](https://github.com/hei-school/hei-admin-api/commit/f2b8345d51d3d1ddf5a1f44f9318893325bbd7be))



# [1.87.0](https://github.com/hei-school/hei-admin-api/compare/v1.86.0...v1.87.0) (2024-12-19)


### Bug Fixes

* take off LATE status by default for fees stats  ([1d5e253](https://github.com/hei-school/hei-admin-api/commit/1d5e253adeb0ff811b84c09b2e6dadf1fa3f00b3))


### Features

* add student_first_name field in fee endpoint response  ([ebc870f](https://github.com/hei-school/hei-admin-api/commit/ebc870f9aa811804cf43acb1b027f64673415d47))
* manually upgrade code version ([54bb3e2](https://github.com/hei-school/hei-admin-api/commit/54bb3e25fad98153dae7d7063cfd8c75fa947292))
* **not-implemented:** verify mpbs via xls file ([d9b6107](https://github.com/hei-school/hei-admin-api/commit/d9b610716c818cb027cea518699c7368abd5af32))
* verify mpbs via xls file provided by orange ([b902d9e](https://github.com/hei-school/hei-admin-api/commit/b902d9e820c5e17e3ce8597d2007148a39b10eda))



# [1.86.0](https://github.com/hei-school/hei-admin-api/compare/v1.85.0...v1.86.0) (2024-12-09)


### Bug Fixes

* use subquery for joining case  ([5e902d9](https://github.com/hei-school/hei-admin-api/commit/5e902d90fa2245a07e269446b4ccf4a6a9e29529))


### Features

* fee stats depends on fee criteria filter   ([e9a8206](https://github.com/hei-school/hei-admin-api/commit/e9a820656f2387d4fdff1da8c6d400c126f445e5))



# [1.85.0](https://github.com/hei-school/hei-admin-api/compare/v1.83.1...v1.85.0) (2024-12-06)


### Bug Fixes

* crupdate staff member access ([b375d3a](https://github.com/hei-school/hei-admin-api/commit/b375d3a547b08f5b9b80d495e17d45e7b9ff076f))


### Features

* filter fees by status and type  ([c838c1b](https://github.com/hei-school/hei-admin-api/commit/c838c1bfa4822a8de03966183ac6211fdbf1ea9a))


### Reverts

* Revert "chore: lower min code coverage(line)" ([8c13767](https://github.com/hei-school/hei-admin-api/commit/8c1376736009c117d09fb558d2ae427348e3b878))



## [1.83.1](https://github.com/hei-school/hei-admin-api/compare/v1.83.0...v1.83.1) (2024-12-04)


### Bug Fixes

* handle null case in role param ([22a9b89](https://github.com/hei-school/hei-admin-api/commit/22a9b89f63b27d4e44fdc918a5b846617f747633))
* letter stats required param ([05ddfe5](https://github.com/hei-school/hei-admin-api/commit/05ddfe53e701fcd608dd5beb838cc05c44405748))
* return admin model in admin resources ([838193c](https://github.com/hei-school/hei-admin-api/commit/838193c289d8c73e3045441c1346d2826f4b3ce3))
* set mpbs creation datetime not null  ([b02447b](https://github.com/hei-school/hei-admin-api/commit/b02447b2e960458df98220dfb52c6c2ccc3c2bd9))
* upload staff member profile picture ([40e730b](https://github.com/hei-school/hei-admin-api/commit/40e730b17fde513cd8c3d660df1ad02276629867))



# [1.83.0](https://github.com/hei-school/hei-admin-api/compare/v1.82.0...v1.83.0) (2024-11-28)


### Bug Fixes

* handle role param null case in letter endpoint ([62d78a3](https://github.com/hei-school/hei-admin-api/commit/62d78a33aa71b03bf6e98438e16314252b53e6c4))


### Features

* letter stats for admin ([6bccb5e](https://github.com/hei-school/hei-admin-api/commit/6bccb5e83617d1ba3f6e897306ad479522c6b866))
* update staff members access ([2cce4b1](https://github.com/hei-school/hei-admin-api/commit/2cce4b1b48261ab853b00fbb869e091d1af23f40))



# [1.82.0](https://github.com/hei-school/hei-admin-api/compare/v1.81.0...v1.82.0) (2024-11-27)


### Features

* implement staff members resources ([58f8580](https://github.com/hei-school/hei-admin-api/commit/58f85805776877f672bac1c59d888dc0f416381e))



# [1.81.0](https://github.com/hei-school/hei-admin-api/compare/v1.80.0...v1.81.0) (2024-11-27)


### Features

* **not-implemented:** add new attribute to fees statistics  ([cd945d4](https://github.com/hei-school/hei-admin-api/commit/cd945d49277bcc05a632403498a24d3491643e5c))




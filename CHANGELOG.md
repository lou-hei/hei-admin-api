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



# [1.80.0](https://github.com/hei-school/hei-admin-api/compare/v1.79.0...v1.80.0) (2024-11-26)


### Bug Fixes

* overpaid fees by mpbs  ([0044dd1](https://github.com/hei-school/hei-admin-api/commit/0044dd1bc8d05d3974d97c71916a0c45f798a966))
* revert  ([a4e2442](https://github.com/hei-school/hei-admin-api/commit/a4e24424211a888cabd97ce539f116ca90321e30))


### Features

* add admin role and update letter access ([17f792f](https://github.com/hei-school/hei-admin-api/commit/17f792f4033517d645659fb4312e43af59fe6400))
* admin profile ([77c16d7](https://github.com/hei-school/hei-admin-api/commit/77c16d71807d469da531852fb636032c264d27a5))
* **not-implemented:** staff member resources ([9042dad](https://github.com/hei-school/hei-admin-api/commit/9042dad655d557b214ec041eb1d60718b85c2815))


### Reverts

* move student file controller to user file controller ([738c681](https://github.com/hei-school/hei-admin-api/commit/738c681cbe253a64dbeed19ae3f5d9fb96e8949d))



# [1.79.0](https://github.com/hei-school/hei-admin-api/compare/v1.78.0...v1.79.0) (2024-11-15)


### Features

* teachers have their own files ([39e5331](https://github.com/hei-school/hei-admin-api/commit/39e533142be417f05b8d353387b23f0cabd5b43a))



# [1.78.0](https://github.com/hei-school/hei-admin-api/compare/v1.77.1...v1.78.0) (2024-11-15)


### Bug Fixes

* if payment amount is more than expected remaining amount  ([67cc919](https://github.com/hei-school/hei-admin-api/commit/67cc919f96a947440360da3c77f431262af6493a))


### Features

* teacher can upload letter ([6902230](https://github.com/hei-school/hei-admin-api/commit/690223033c50fc43a5efb19ee314784edbaa660d))



## [1.77.1](https://github.com/hei-school/hei-admin-api/compare/v1.77.0...v1.77.1) (2024-11-14)


### Bug Fixes

* make mpbs idempotant ([27ab987](https://github.com/hei-school/hei-admin-api/commit/27ab98709d157a64578d47fa0ccd42e502410b1e))



# [1.77.0](https://github.com/hei-school/hei-admin-api/compare/v1.76.0...v1.77.0) (2024-11-13)


### Features

* implement grades resources ([e370dad](https://github.com/hei-school/hei-admin-api/commit/e370dad2ba64c22a85a849bcb1dee52b7a32973c))



# [1.76.0](https://github.com/hei-school/hei-admin-api/compare/v1.75.0...v1.76.0) (2024-11-08)


### Bug Fixes

* fetch only for enabled and suspended students  ([08ed33a](https://github.com/hei-school/hei-admin-api/commit/08ed33a1b9a1e3823e7442c8e2854b13927fb1db))


### Features

* filter exams and get exam by id ([834db9d](https://github.com/hei-school/hei-admin-api/commit/834db9d1fad3e57e3714585be9a1adeb3317a2f9))



# [1.75.0](https://github.com/hei-school/hei-admin-api/compare/v1.70.0...v1.75.0) (2024-11-07)


### Bug Fixes

* course id null while creating an event ([b63feb9](https://github.com/hei-school/hei-admin-api/commit/b63feb901e6536788d61a34159fb8e5583e8b88b))
* disbale test failing ([34eea1e](https://github.com/hei-school/hei-admin-api/commit/34eea1ec60f05f888926520bfca24643b4ca21bb))
* security conf for students promotin generation  ([3bb12a0](https://github.com/hei-school/hei-admin-api/commit/3bb12a0ff1a0680da6fe617e69ec3d65eb732a43))
* update status after mpbs  ([408676a](https://github.com/hei-school/hei-admin-api/commit/408676a1052341e6f38464a166b9931c7d9d50b1))
* xlsx cell instant format  ([3a7276d](https://github.com/hei-school/hei-admin-api/commit/3a7276d97df5dcdce5cbfa5fdd4d98007f87df15))


### Features

* generate fees as xlsx  ([c124501](https://github.com/hei-school/hei-admin-api/commit/c124501336010598be3c802fd395aafd11942abf))
* generate students group in xlsx ([6922fed](https://github.com/hei-school/hei-admin-api/commit/6922feddd8f07dd3b5aedac9b9476e09cea4a991))
* generate students promotion in xlsx  ([ca7ef7e](https://github.com/hei-school/hei-admin-api/commit/ca7ef7ebcc478a18907d7184fbe0a28f7cfde393))
* **not-implemented:** add endpoint specific for exams creation and retrieve ([56063df](https://github.com/hei-school/hei-admin-api/commit/56063df42d44a2b9447afcd9e28b2a18df39bd6a))
* **not-implemented:** get monthy fees statistics ([cff0772](https://github.com/hei-school/hei-admin-api/commit/cff07722f3364fcb839366b980a722e56956cceb))
* stats is available in event model ([c489d9e](https://github.com/hei-school/hei-admin-api/commit/c489d9ed86d3358bbabccc553ba35bcbe441f20f))




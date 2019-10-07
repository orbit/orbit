module.exports = (router) => {
  router.use('/users', require('./users'))

  router.use('/messages', require('./messages'))

}

const Router = require('koa-router')
const router = new Router()
const Ctrl = require('../controllers/users')

router.get('/', ctx => ctx.ok(Ctrl.hello(ctx.request.query.user)))

module.exports = router.routes()

const Router = require('koa-router')
const router = new Router()
const MessagesController = require('../controllers/messages').default
const messagesController = new MessagesController('./proto', "orbit-server-1:50056")

router.get('/', async ctx => ctx.ok(await messagesController.send(ctx.request.query.address, ctx.request.query.message)))

module.exports = router.routes()

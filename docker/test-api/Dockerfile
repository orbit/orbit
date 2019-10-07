FROM node:12

ENV TARGETDIR /opt/orbit/api
ENV NODE_PATH=$TARGETDIR
ENV ASSETS_PATH=/public
ENV APP_PORT=3000

EXPOSE $APP_PORT

ADD ./src/orbit-test-api/package.json $TARGETDIR/
WORKDIR $TARGETDIR

RUN yarn

CMD npm run dev

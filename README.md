# Simple rest service

Service uses Vert.x library.
You can register new users, then you can login and obtain token to make calls. Further, while authenticating with token, you can save items and retrieve items list that has been saved.

There is need to place ```keystore.jceks``` file under root directory (generating token feature).
You can execute command: ```keytool -genkeypair -alias myalias -keyalg RSA -keysize 2048 -validity 365 -keystore keystore.jceks```

For development purpose use ```docker-compose.yaml``` file to start MongoDB container.

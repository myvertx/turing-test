# -n    即requests，用于指定压力测试总共的执行次数
# -c    即concurrency，用于指定压力测试的并发数
# -k    即keep-alive，保持连接，避免超时
# -r    在遇到socket接收错误后，不退出测试
# -s    最大超时时间，默认30s
# -t    即timelimit，等待响应的最大时间(单位：秒)
# -v 4  查看具体的请求和响应
# -l    忽略返回长度不一致的问题
ab -n 10000 -c 100 -k -r -s 999999999 -l -p "test-verify.json" -T "application/json" "http://127.0.0.1:8888/captcha/verify?id=1Ln0tTaOQA8x-QmxIv8e2"

# -n 即requests，用于指定压力测试总共的执行次数。
# -c 即concurrency，用于指定压力测试的并发数。
# -t 即timelimit，等待响应的最大时间(单位：秒)
# -l 忽略返回长度不一致的问题
ab -n 100 -c 100 -l http://127.0.0.1:9000/captcha/gen

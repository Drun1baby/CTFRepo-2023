FROM python:3.5

COPY ./flag /flag
COPY ./web /app

RUN pip install -i https://pypi.tuna.tsinghua.edu.cn/simple -r /app/requirements.txt 


WORKDIR /app

EXPOSE 80

CMD ["python3","manage.py","runserver", "0.0.0.0:80"]

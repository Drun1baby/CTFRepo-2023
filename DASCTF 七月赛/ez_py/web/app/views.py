from django.contrib.auth import authenticate, login, logout
from django.contrib.auth.decorators import login_required
from django.shortcuts import render, redirect,render_to_response
from django.urls import reverse
from django.views.decorators.csrf import csrf_exempt
from django.conf import settings
from django.shortcuts import HttpResponse
name = "Django"


def login_view(request):

    return render_to_response('app/login.html')
def auth_view(request, onsuccess='/', onfail='/error'):
    username = request.POST["username"]
    password = request.POST["password"]
    user = authenticate(request, username=username, password=password)
    if user is not None:
        login(request, user)
        return redirect(onsuccess)
    else:
        return redirect(onfail)
def error_view(request):
    return render_to_response("app/error.html")


def index_view(request):
    # initialize
    return render_to_response("app/index.html")






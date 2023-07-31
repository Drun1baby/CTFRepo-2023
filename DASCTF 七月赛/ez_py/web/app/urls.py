from django.urls import path

from . import views

urlpatterns = [
    path('', views.index_view, name='index'),
    path('login', views.login_view, name='login'),
    path('auth', views.auth_view, name='auth'),
    path('error', views.error_view, name='error')
]

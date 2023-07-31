<?php
error_reporting(0);
highlight_file(__FILE__);
class evil{
    public $cmd;
    public $a;
    public function __destruct(){
        if('VanZZZZY' === preg_replace('/;+/','VanZZZZY',preg_replace('/[A-Za-z_\(\)]+/','',$this->cmd))){
            eval($this->cmd.'givemegirlfriend!');
        } else {
            echo 'nonono';
        }
    }
}

if(!preg_match('/^[Oa]:[\d]+|Array|Iterator|Object|List/i',$_GET['Pochy'])){
    unserialize($_GET['Pochy']);
} else {
    echo 'nonono';
}
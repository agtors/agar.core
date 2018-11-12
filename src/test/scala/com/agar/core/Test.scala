package com.agar.core

class Pizza {

  var crustSize = 12
  var crustType = "Thin"

  def this(crustSize: Int) {
    this()
    this.crustSize = crustSize
  }

  def this(crustSize: Int, crustType: String) {
    this(crustSize)
    this.crustType = crustType
  }

  override def toString = {
    "A %s inch pizza with %s crust.".format(crustSize, crustType)
  }

}
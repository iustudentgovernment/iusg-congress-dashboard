package edu.iu.iustudentgovernment.utils

fun String.nullifyEmpty() = if (isEmpty()) null else this
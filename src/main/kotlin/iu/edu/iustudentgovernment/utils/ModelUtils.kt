package iu.edu.iustudentgovernment.utils

import spark.ModelAndView
import spark.template.handlebars.HandlebarsTemplateEngine

fun HandlebarsTemplateEngine.render(map: Map<String, Any?>, name: String) = render(ModelAndView(map, name))
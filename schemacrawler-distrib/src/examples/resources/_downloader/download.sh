#!/bin/sh
java -jar ivy-2.4.1.jar -ivy $1_ivy.xml -retrieve "../_schemacrawler/lib/[artifact]-[revision]-[type].[ext]"
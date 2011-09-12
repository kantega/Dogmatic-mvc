/*
 * Copyright 2011 Kantega AS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kantega.dogmaticmvc.scala

import tools.nsc.reporters.ConsoleReporter
import tools.nsc.{Settings, Global}
import javax.servlet.http.HttpServletRequest
import javax.servlet.ServletContext
import java.lang.String
import java.io.File
import tools.nsc.io.{PlainFile, AbstractFile, VirtualDirectory}
import org.kantega.dogmaticmvc.api.ScriptCompiler
import io.Source
import java.net.{URLDecoder, URLClassLoader, URL}
import collection.mutable.MutableList

/**
 *
 */

class ScalaScriptCompiler(val servletContext:ServletContext) extends ScriptCompiler {

  val virtualDirectory = new VirtualDirectory("(memory)", None)

  
  def getSource(req: HttpServletRequest) : URL = {
    servletContext.getResource("/WEB-INF/dogmatic" + req.getServletPath() + ".scala");
  }

  def getCompiledClassBytes(req: HttpServletRequest) :java.util.Map[String,Array[Byte]] = {
    val bytes = new java.util.HashMap[String, Array[Byte]]
    virtualDirectory.foreach((file:AbstractFile) => bytes.put(file.name.substring(0, file.name.lastIndexOf(".")), file.toByteArray))
    return bytes
  }

  def compile(request: HttpServletRequest) : java.lang.Class[_] = {
    var settings = new Settings()

    settings.usejavacp.value = true

    settings.outputDirs setSingleOutput virtualDirectory

    var cp: String = ""


    for (cl <- List(getClass.getClassLoader, getClass().getClassLoader.getParent)) {
      if (cl.isInstanceOf[URLClassLoader]) {
        var ucl: URLClassLoader = cl.asInstanceOf[URLClassLoader]
        for (url <- ucl.getURLs) {
          if (cp.length > 0) {
            cp += File.pathSeparator
          }
          cp += url.getFile
        }
      }
    }
    settings.classpath.value = cp;

    val reporter = new ConsoleReporter(settings)
    val glob = new Global(settings, reporter)

    val run = new glob.Run()

    var url: URL = getSource(request)
    val code = Source.fromInputStream(url.openStream, "utf-8").getLines.mkString

    var mainFile: File = new File(URLDecoder.decode(url.getFile, "utf-8"))

    var sourceFiles : List[AbstractFile] = List()

    mainFile.getParentFile.listFiles.toList
      .filter(_.getName.endsWith(".scala"))
      .foreach((file:File) => sourceFiles ::= new PlainFile(file.getAbsolutePath))

    run.compileFiles(sourceFiles)

    if(reporter.hasErrors) {
      throw new RuntimeException("Compilation failed")
    }



    val cl = new ClassLoader(getClass.getClassLoader) {
      override def findClass(p1: String) = {
        var directory: VirtualDirectory = virtualDirectory
        var file: AbstractFile = directory.lookupName(p1 + ".class", false)
        if( file == null) {
          throw new ClassNotFoundException(p1)
        }
        val bytes = file.toByteArray
        defineClass(p1, bytes, 0, bytes.length)
      }
    }

    cl.loadClass(request.getServletPath.substring(1));

  }

  def canHandle(request: HttpServletRequest) = {
    getSource(request) != null
  }

  def main(args: Array[String]) {

  }
}
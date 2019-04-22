package com.thoughtworks.modularizer.views.homepage.importtab

import com.thoughtworks.binding.{Binding, LatestEvent, dom}
import org.scalajs.dom.FileReader
import org.scalajs.dom.raw.{HTMLDivElement, HTMLInputElement}

/**
  * @author 杨博 (Yang Bo)
  */
class DotFileInputGroup {

  @dom
  private val input: Binding[HTMLInputElement] = <input
    id="jdependReportFile"
    type="file"
    placeholder="Select JDepend report"
    required="required"
    accept="text/vnd.graphviz"
  />

  private val readerOption: Binding[Option[FileReader]] = Binding {
    val _ = LatestEvent.change(input.bind).bind
    if (input.bind.files.length == 1) {
      Some(new FileReader)
    } else {
      None
    }
  }

  val loadedText: Binding[Option[String]] = Binding {
    readerOption.bind match {
      case Some(reader) =>
        reader.readAsText(input.bind.files(0))
        val _ = LatestEvent.load(reader).bind
        Some(reader.result.toString)
      case None => None
    }
  }

  @dom
  val view: Binding[HTMLDivElement] =
    <div class="form-group">
      <label for="jdependReportFile">Import DOT output file produced by <kbd>jdeps</kbd></label>
      {
        val element = input.bind
        element.className = raw"""
          form-control
          ${
            readerOption.bind match {
              case None => ""
              case Some(reader) =>
                LatestEvent.error(reader).bind match {
                  case None =>
                    LatestEvent.progress(reader).bind match {
                      case None =>
                        ""
                      case Some(event) =>
                        "is-valid"
                    }
                  case Some(event) =>
                    "is-invalid"
                }
            }
          }
        """
        element
      }
      <small class="form-text text-muted">
        You can run command-line tool <kbd>jdeps -dotoutput your.jar</kbd> from OpenJDK
        to produce the dependency report <code>your.jar.dot</code>.
      </small>
      {
        readerOption.bind match {
          case None =>
            <!-- No file is loading -->
          case Some(reader) =>
            LatestEvent.error(reader).bind match {
              case None =>
                LatestEvent.progress(reader).bind match {
                  case None =>
                    <!-- No progress information -->
                  case Some(event) =>
                    <div class="valid-feedback">{event.loaded.toString} / {event.total.toString} bytes loaded</div>
                }
              case Some(event) =>
                <div class="invalid-feedback">{reader.error.toString}</div>
            }
        }
      }
    </div>
}

package com.thoughtworks.modularizer.view
import com.thoughtworks.binding.Binding.Var
import com.thoughtworks.binding.{LatestEvent, dom}
import com.thoughtworks.modularizer.model.PageState.WorkBoardState
import com.thoughtworks.modularizer.model.{JdepsGraph, PageState}
import org.scalajs.dom.{Event, FileList, FileReader, UIEvent}
import typings.graphlibDashDotLib.graphlibDashDotMod
import typings.graphlibLib.graphlibMod.Graph

/**
  * @author 杨博 (Yang Bo)
  */
object ImportJdepsDotFile {

  @dom
  def render(pageState: Var[PageState], outputGraph: Var[Option[Graph]]) = {
    val readerOption: Var[Option[FileReader]] = Var(None)
    <div class="container-fluid">
      <div class="card">
        <div class="card-body">
          <h5 class="card-title">Import dependencies</h5>
          <form>
            <div class="form-group">
              <label for="jdependReportFile">Import DOT output file produced by <kbd>jdeps</kbd></label>
              <input id="jdependReportFile" type="file" class="form-control" placeholder="Select JDepend report"
                accept="text/vnd.graphviz"
                onchange={ _: Event =>
                  val files: FileList = jdependReportFile.files
                  if (files.length == 1) {
                    val reader = new FileReader
                    readerOption.value = Some(reader)
                    reader.onload = { _: UIEvent =>
                      val jdepsGraph = JdepsGraph(graphlibDashDotMod.^.read(reader.result.toString))

                      val graph = jdepsGraph.internalDependencies
                      outputGraph.value = Some(graph)
                      pageState.value = PageState.WorkBoard(WorkBoardState.Summary)
                    }
                    reader.readAsText(files(0))
                  } else {
                    readerOption.value = None
                  }
                }
              />
              <small class="form-text text-muted">
                You can run command-line tool <kbd>jdeps --dot-output</kbd> from OpenJDK to produce the dependency report <code>your.jar.dot</code> for <code>your.jar</code>
              </small>
            </div>
          </form>
        </div>
      </div>
      {
        readerOption.bind match {
          case None =>
            <!-- No file is loading -->
          case Some(reader) =>
            loadingStatus(reader).bind
        }
      }
    </div>
  }

  @dom
  def loadingStatus(reader: FileReader) = <div>
    {
      LatestEvent.progress(reader).bind match {
        case None =>
          <!-- No progress information -->
        case Some(event) =>
          <div>{event.loaded.toString} / {event.total.toString} bytes loaded</div>
      }
    }
    {
      LatestEvent.error(reader).bind match {
        case None =>
          <!-- No Error -->
        case Some(event) =>
          <div>{reader.error.toString}</div>
      }
    }
  </div>
}

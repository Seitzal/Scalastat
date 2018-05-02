package seitzal.scalastat.plot

import java.awt._
import java.awt.event._
import javax.swing._

/**
 * Abstract superclass for all plot types.
 */
abstract class Plot {
  val width : Int
  val height : Int
  val title : String
  def draw (g : Graphics)
  def show {
    val panel = new JPanel {
      override def paintComponent(g : Graphics) { draw(g) }
    }
    panel.setPreferredSize(new Dimension(width, height))
    val frame = new JFrame(title)
    frame.add(panel)
    frame.pack
    frame.setVisible(true)
  }
}

/**
 * Dummy class for fast creation of simple, custom plots.
 * For more complex applications, it is preferred to create a new child class of [[seitzal.scalastat.plot.Plot]]
 */
class CustomPlot (p_width : Int, p_height : Int, p_title : String, drawer : (Graphics) => Unit) extends Plot {
  override val width = p_width
  override val height = p_height
  override val title = p_title
  override def draw (g : Graphics) {
    drawer(g)
  }
}
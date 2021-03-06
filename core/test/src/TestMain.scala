package eu.seitzal.test.insight

import org.scalatest._
import eu.seitzal.insight._

class TestMain extends FunSuite with Tags with EnvGen {

  test("Cumulative absolute frequencies", Freq) {
    new EnvFreq {
      val cf = freq.num("Anzahl").cabsfreq
      assert(
        cf.values(cf.values.length - 1).get == freq.num("Anzahl").sum
      )
    }
  }

  test("Gini index for unaggregated data", Distri) {
    new EnvDistri {
      assert((data1 num("Umsatz") gini) === 0.432)
      assert((data1 num("Umsatz") ngini) === 0.48)
    }
  }

  test("Gini index for unaggregated relative frequencies", Distri) {
    new EnvDistri {
      assert(data1.num("Umsatz").relfreq.gini === 0.432)
      assert(data1.num("Umsatz").relfreq.ngini === 0.48)
    }
  }

  test("Gini index for aggregated data", Distri) {
    new EnvDistri {
      assert(data2.num("Prozent der Arbeitnehmer").cgini === 0.424) 
    }
  }

  test("Robin Hood index", Distri) {
    val xs = NumSeries(2.5,0.5,0.5,0.5)
    assert(xs.rhi == 1.5 / 4)
  }

  test("Concentration rate", Distri) {
    val xs = NumSeries(30, 50, 10, 12, 100, 7, 8)
    assert(xs.crate(3) === 0.8294931)
  }

  test("Herfindahl index 1", Distri) {
    new EnvAggr {
      assert(ratings.num("Rating").herfindahl === 0.0469612)
    }
  }

  test("Herfindahl index 2", Distri) {
    new EnvDistri {
      assert(data3.num("Neuzulassungen").herfindahl === 0.228056)
    }
  }

  test("Skewness", Distri) {
    new EnvAggr {
      assert(Helper.roundTo(ratings.num("Rating").skewness, 3) == -0.539)
    }
  }

  test("Kurtosis", Distri) {
    new EnvAggr {
      assert(Helper.roundTo(ratings.num("Rating").kurtosis, 3) == -0.292)
    }
  }

  test("Covariance", Correl) {
    new EnvCorrel {
      val covxy = data1.cov("X", "Y")
      assert(Helper.roundTo(covxy, 2) == 7.94)
    }
  }

  test("Pearson's r", Correl) {
    new EnvCorrel {
      val r = data1.pearson("X", "Y")
      assert(Helper.roundTo(r, 3) == 0.856)
    }
  }

  test("Partial correlation", Correl) {
    new EnvCorrel {
      val xyunderz = data1.pearsonPartial("X", "Y", "Z")
      assert(Helper.roundTo(xyunderz, 3) == 0.885)
    }
  }

  test("Simple linear regression", Regression) {
    new EnvCorrel {
      val reg = regression.SimpleOLS.build(data2, "GDP/Capita",  "Footprint")
      assert(Helper.roundTo(reg.intersect, 3) == 1.199)
      assert(Helper.roundTo(reg.slope, 6) == 0.000148)
      assert(Helper.roundTo(reg.estimate(8000), 3) == 2.383)
    }
  }

  test("R-squared for simple linear regression", Regression) {
    new EnvCorrel {
      val reg = regression.SimpleOLS.build(data2, "GDP/Capita",  "Footprint")
      assert(Helper.roundTo(reg.rsq, 3) == 0.349)
    }
  }

  test("Common existing length", MissingVals) {
    new EnvMissingVals {
      assert(data.num("B").commonExistingLength(data.num("C")) == 3)
    }
  }

  test("Common existing values", MissingVals) {
    new EnvMissingVals {
      assert(data.num("B").commonExistingValues(data.num("C")) ==
        (Vector(3.5, 3.0, 3.0), Vector(12.0, 0.4, 2.4)))
    }
  }

  test("quantiles", Aggr, Slow, NoJenkins) {
    new EnvQOG {
      val filtered = qog $ ("year", "cname", "undp_hdi") filter ("year", 2010)
      assert(filtered.quantile("undp_hdi", 10, 0.85) == 9)
    }
  }

  test("row extraction") {
    new EnvMissingVals {
      assert(data(0) == Vector("A" -> 1, "B" -> 3.5, "C" -> 12))
      assert(data(1) == Vector("A" -> "n.a.", "B" -> 3, "C" -> 0.4))
      assertThrows[IndexOutOfBoundsException] {
        data(6)
      }
    }
  }

  test("Lazy frame, load columns", Lazy) {
    val qog = LazyFrame.fromCSV("testdata/qog_bas_ts_jan18.csv")
    val undp_hdi = qog.num("undp_hdi")
    assert(undp_hdi.length == 15192)
    assert(undp_hdi.existingLength == 4259)
    assertThrows[NotNumericException] {
      qog.num("cname")
    }
    assert(qog("cname").length == 15192)
  }

    test("lazy frame, Pearson's r for large dataset", Correl, Lazy) {
    val qog = LazyFrame.fromCSV("testdata/qog_bas_ts_jan18.csv")
    val r = qog.num("rsf_pfi") pearson qog.num("wef_ptp")
    assert(Helper.roundTo(r, 4) == -0.0799)
    val r2 = qog.num("wdi_gdpcapcon2010") pearson qog.num("undp_hdi")
    assert(Helper.roundTo(r2, 3) == 0.69)
  }

  test("Contingency table", Freq) {
    val data = read.csv("testdata/freq2.csv")
    val table = new ContingencyTable(data("A"), data("B"))
    assert(table.rowLabels.toList == List("1", "2", "3", "4", "5"))
    assert(table.colLabels.toList == List("0", "1", "2"))
    assert(table.rowTotal.toList == List(100, 85, 95, 130, 90))
    assert(table.colTotal.toList == List(240, 140, 120))
    assert(table.grandTotal == 500)
    assert(Helper.round(table.rel("1", "0")) == 0.17)
    assert(Helper.round(table.relRowTotal(0)) == 0.2)
    assert(Helper.round(table.relColTotal(0)) == 0.48)
    assert(Helper.round(table.rowProp("1", "0")) == 0.85)
    assert(Helper.roundTo(table.colProp("1", "0"), 4) == 0.3542)
  }
}

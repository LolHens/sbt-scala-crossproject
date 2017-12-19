lazy val check = taskKey[Unit]("check settings are applied")

def assertEquals[T](actual: T, expected: T): Unit = {
  assert(actual == expected, s"actual: $actual, expected: $expected")
}

lazy val foo =
  crossProject(JVMPlatform.scala("2.11"), JVMPlatform.scala("2.12"))
    .crossType(CrossType.Pure)

lazy val `foo-2.11` = foo.scala("2.11").jvm
lazy val `foo-2.12` = foo.scala("2.12").jvm

check := {
  def expected(scala: String, config: String ) = Seq(
    baseDirectory.value / "foo" / s".jvm-$scala" / "src" / config / "resources",
    baseDirectory.value / "foo" / "src" / config / "resources"
  )
  assertEquals((unmanagedResourceDirectories in(`foo-2.11`, Compile)).value, expected("2.11", "main"))
  assertEquals((unmanagedResourceDirectories in(`foo-2.11`, Test)).value, expected("2.11", "test"))
  assertEquals((unmanagedResourceDirectories in(`foo-2.12`, Compile)).value, expected("2.12", "main"))
  assertEquals((unmanagedResourceDirectories in(`foo-2.12`, Test)).value, expected("2.12", "test"))
}

package com.dwai.idebridge.api

import com.dwai.idebridge.impl.*

object ApiRegistry {
    var editor: EditorApi = EditorImpl
    var project: ProjectApi = ProjectImpl
    var run: RunApi = RunImpl
    var terminal: TerminalApi = TerminalImpl
    var problem: ProblemApi = ProblemImpl
    var navigate: NavigateApi = NavigateImpl
    var diff: DiffApi = DiffImpl
    var file: FileApi = FileImpl
}
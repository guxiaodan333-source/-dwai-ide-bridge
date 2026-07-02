package com.dwai.idebridge.bridge

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement

object PsiBridge {

    fun walkStructure(
        element: PsiElement,
        document: com.intellij.openapi.editor.Document?,
        result: MutableList<Map<String, Any>>,
        depth: Int
    ) {
        if (depth > 3) return

        for (child in element.children) {
            val namedElement = child as? PsiNamedElement ?: continue
            val name = namedElement.name ?: continue
            if (name.startsWith("<") && name.endsWith(">")) continue

            val typeName = child.node?.elementType?.toString()
                ?.removePrefix("Py:")
                ?.removePrefix("java:")
                ?: "element"

            val line = try {
                if (document != null) {
                    val offset = child.textRange?.startOffset ?: -1
                    if (offset >= 0) document.getLineNumber(offset) else -1
                } else -1
            } catch (_: Exception) { -1 }

            result.add(mapOf(
                "type" to typeName,
                "name" to name,
                "line" to line,
                "depth" to depth
            ))
            walkStructure(child, document, result, depth + 1)
        }
    }
}
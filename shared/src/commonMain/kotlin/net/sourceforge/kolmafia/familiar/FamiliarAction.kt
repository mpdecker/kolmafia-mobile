package net.sourceforge.kolmafia.familiar

sealed class FamiliarAction {
    data class PocketProfessorLecture(val lectureId: Int) : FamiliarAction()
    data class ShortestWigAssignment(val colorId: Int) : FamiliarAction()
    object Unsupported : FamiliarAction()
}

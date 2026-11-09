package dev.qtremors.osyster.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectedRowShapeTest {
    @Test
    fun singleRowIsFullyRounded() {
        assertEquals(RoundedCornerShape(GroupCorner), connectedRowShape(0, 1))
    }

    @Test
    fun middleRowUsesInnerCorners() {
        assertEquals(RoundedCornerShape(RowCorner), connectedRowShape(1, 3))
    }

    @Test
    fun firstAndLastRowsRoundOnlyTheOutside() {
        assertEquals(
            RoundedCornerShape(topStart = GroupCorner, topEnd = GroupCorner, bottomStart = RowCorner, bottomEnd = RowCorner),
            connectedRowShape(0, 3)
        )
        assertEquals(
            RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
            connectedRowShape(2, 3)
        )
    }
}

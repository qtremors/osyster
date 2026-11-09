# Osyster release shrinking rules.
#
# Libraries ship their own consumer rules: kotlinx.serialization keeps generated serializers and
# companions, Hilt keeps its components, and Navigation resolves type-safe routes through those
# serializers. Persisted JSON keys and route names come from compile-time serial names, so
# renaming classes does not change them. Only add a rule here for code reached by reflection.

# Readable crash traces without shipping original file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Add -dontwarn lines for optional library classes the app does not ship, each with a comment
# saying which library references them and why they are absent.

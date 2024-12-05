package school.hei.haapi.file.hash;

import school.hei.haapi.PojaGenerated;

@PojaGenerated
public record FileHash(FileHashAlgorithm algorithm, String value) {}

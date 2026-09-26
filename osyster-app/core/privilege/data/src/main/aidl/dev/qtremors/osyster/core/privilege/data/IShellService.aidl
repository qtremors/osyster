package dev.qtremors.osyster.core.privilege.data;

// Runs inside Shizuku's process as the shell user. Shizuku calls destroy() with this fixed code
// when it unbinds; exec returns [exit code, standard output, standard error].
interface IShellService {
    void destroy() = 16777114;

    String[] exec(String command) = 1;
}

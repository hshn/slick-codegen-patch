{
  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixpkgs-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs { inherit system; };
        jdk = pkgs.temurin-bin-21;
        shellPackages = with pkgs; [
          temurin-bin-21
          (pkgs.sbt.override { jre = pkgs.temurin-bin-21; })
        ];
      in {
        devShells.default = pkgs.mkShell {
          buildInputs = shellPackages;
        };
      });
}

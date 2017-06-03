#!/bin/sh

# This script adds a menu item, icons and mime type for Processing for 
# the current user. If possible, it will use the xdg-utils - or fall back 
# to just creating and copying a desktop file to the user's directory.
# If called with the "-u" option, it will uninstall.

# Resource name to use (including vendor prefix)
RESOURCE_NAME=processing-pde

# Get absolute path from which this script file was executed
# (Could be changed to "pwd -P" to resolve symlinks to their target)
SCRIPT_PATH=$( cd $(dirname $0) ; pwd )
cd "${SCRIPT_PATH}"

# Default mode is to install.
UNINSTALL=false

# If possible, get location of the desktop folder. Default to ~/Desktop
XDG_DESKTOP_DIR="${HOME}/Desktop"
if [ -f "${XDG_CONFIG_HOME:-${HOME}/.config}/user-dirs.dirs" ]; then
  . "${XDG_CONFIG_HOME:-${HOME}/.config}/user-dirs.dirs"
fi

# Install using xdg-utils
xdg_install_f() {

  # Create a temp dir accessible by all users
  TMP_DIR=`mktemp --directory`

  # Create *.desktop file using the existing template file
  sed -e "s,<BINARY_LOCATION>,${SCRIPT_PATH}/processing,g" \
      -e "s,<ICON_NAME>,${RESOURCE_NAME},g" "${SCRIPT_PATH}/lib/desktop.template" > "${TMP_DIR}/${RESOURCE_NAME}.desktop"

  # Install the icon files using name and resolutions
  xdg-icon-resource install --context mimetypes --size 16 "${SCRIPT_PATH}/lib/icons/pde-16.png" $RESOURCE_NAME
  xdg-icon-resource install --context mimetypes --size 32 "${SCRIPT_PATH}/lib/icons/pde-32.png" $RESOURCE_NAME
  xdg-icon-resource install --context mimetypes --size 48 "${SCRIPT_PATH}/lib/icons/pde-48.png" $RESOURCE_NAME
  xdg-icon-resource install --context mimetypes --size 64 "${SCRIPT_PATH}/lib/icons/pde-64.png" $RESOURCE_NAME
  xdg-icon-resource install --context mimetypes --size 128 "${SCRIPT_PATH}/lib/icons/pde-128.png" $RESOURCE_NAME
  xdg-icon-resource install --context mimetypes --size 256 "${SCRIPT_PATH}/lib/icons/pde-256.png" $RESOURCE_NAME
  xdg-icon-resource install --context mimetypes --size 512 "${SCRIPT_PATH}/lib/icons/pde-512.png" $RESOURCE_NAME
  xdg-icon-resource install --context mimetypes --size 1024 "${SCRIPT_PATH}/lib/icons/pde-1024.png" $RESOURCE_NAME

  # Install the created *.desktop file
  xdg-desktop-menu install "${TMP_DIR}/${RESOURCE_NAME}.desktop"

  # Create icon on the desktop
  xdg-desktop-icon install "${TMP_DIR}/${RESOURCE_NAME}.desktop"

  # Install Processing mime type
  xdg-mime install "${SCRIPT_PATH}/lib/${RESOURCE_NAME}.xml"

  # Install icons for mime type
  xdg-icon-resource install --context mimetypes --size 16 "${SCRIPT_PATH}/lib/icons/pde-16.png" text-x-processing
  xdg-icon-resource install --context mimetypes --size 32 "${SCRIPT_PATH}/lib/icons/pde-32.png" text-x-processing
  xdg-icon-resource install --context mimetypes --size 48 "${SCRIPT_PATH}/lib/icons/pde-48.png" text-x-processing
  xdg-icon-resource install --context mimetypes --size 64 "${SCRIPT_PATH}/lib/icons/pde-64.png" text-x-processing
  xdg-icon-resource install --context mimetypes --size 128 "${SCRIPT_PATH}/lib/icons/pde-128.png" text-x-processing
  xdg-icon-resource install --context mimetypes --size 256 "${SCRIPT_PATH}/lib/icons/pde-256.png" text-x-processing
  xdg-icon-resource install --context mimetypes --size 512 "${SCRIPT_PATH}/lib/icons/pde-512.png" text-x-processing
  xdg-icon-resource install --context mimetypes --size 1024 "${SCRIPT_PATH}/lib/icons/pde-1024.png" text-x-processing

  # Make the Processing Development Environment the default app for *.pde files
  xdg-mime default ${RESOURCE_NAME}.desktop text/x-processing

  # Clean up
  rm "${TMP_DIR}/${RESOURCE_NAME}.desktop"
  rmdir "$TMP_DIR"

}

# Install by simply copying desktop file (fallback)
simple_install_f() {

  # Create a temp dir accessible by all users
  TMP_DIR=`mktemp --directory`

  # Create *.desktop file using the existing template file
  sed -e "s,<BINARY_LOCATION>,${SCRIPT_PATH}/processing,g" \
      -e "s,<ICON_NAME>,${SCRIPT_PATH}/lib/icons/pde-128.png,g" "${SCRIPT_PATH}/lib/desktop.template" > "${TMP_DIR}/${RESOURCE_NAME}.desktop"

  mkdir -p "${HOME}/.local/share/applications"
  cp "${TMP_DIR}/${RESOURCE_NAME}.desktop" "${HOME}/.local/share/applications/"

  mkdir -p "${HOME}/.local/share/metainfo"
  cp "${SCRIPT_PATH}/lib/appdata.xml" "${HOME}/.local/share/metainfo/${RESOURCE_NAME}.appdata.xml"

  # Copy desktop icon if desktop dir exists (was found)
  if [ -d "${XDG_DESKTOP_DIR}" ]; then
   cp "${TMP_DIR}/${RESOURCE_NAME}.desktop" "${XDG_DESKTOP_DIR}/"
   # Altering file permissions to avoid "Untrusted Application Launcher" error on Ubuntu
   chmod u+x "${XDG_DESKTOP_DIR}/${RESOURCE_NAME}.desktop"
  fi

  # Clean up temp dir
  rm "${TMP_DIR}/${RESOURCE_NAME}.desktop"
  rmdir "${TMP_DIR}"

}

# Uninstall using xdg-utils
xdg_uninstall_f() {

  # Remove *.desktop file
  xdg-desktop-menu uninstall ${RESOURCE_NAME}.desktop

  # Remove icon from desktop
  xdg-desktop-icon uninstall ${RESOURCE_NAME}.desktop

  # Remove icons
  xdg-icon-resource uninstall --size 16 $RESOURCE_NAME
  xdg-icon-resource uninstall --size 32 $RESOURCE_NAME
  xdg-icon-resource uninstall --size 48 $RESOURCE_NAME
  xdg-icon-resource uninstall --size 64 $RESOURCE_NAME
  xdg-icon-resource uninstall --size 128 $RESOURCE_NAME
  xdg-icon-resource uninstall --size 256 $RESOURCE_NAME
  xdg-icon-resource uninstall --size 512 $RESOURCE_NAME
  xdg-icon-resource uninstall --size 1024 $RESOURCE_NAME

  # Remove MIME type icons
  xdg-icon-resource uninstall --size 16 text-x-processing
  xdg-icon-resource uninstall --size 32 text-x-processing
  xdg-icon-resource uninstall --size 48 text-x-processing
  xdg-icon-resource uninstall --size 64 text-x-processing
  xdg-icon-resource uninstall --size 128 text-x-processing
  xdg-icon-resource uninstall --size 256 text-x-processing
  xdg-icon-resource uninstall --size 512 text-x-processing
  xdg-icon-resource uninstall --size 1024 text-x-processing

  # Remove MIME type
  xdg-mime uninstall "${SCRIPT_PATH}/lib/${RESOURCE_NAME}.xml"

}

# Uninstall by simply removing desktop files (fallback), incl. old one
simple_uninstall_f() {

  # delete legacy cruft .desktop file
  if [ -f "${HOME}/.local/share/applications/processing.desktop" ]; then
    rm "${HOME}/.local/share/applications/processing.desktop"
  fi

  # delete another legacy .desktop file
  if [ -f "${HOME}/.local/share/applications/processing-processingide.desktop" ]; then
    rm "${HOME}/.local/share/applications/processing-processingide.desktop"
  fi

  if [ -f "${HOME}/.local/share/applications/${RESOURCE_NAME}.desktop" ]; then
    rm "${HOME}/.local/share/applications/${RESOURCE_NAME}.desktop"
  fi

  if [ -f "${HOME}/.local/share/metainfo/${RESOURCE_NAME}.appdata.xml" ]; then
    rm "${HOME}/.local/share/metainfo/${RESOURCE_NAME}.appdata.xml"
  fi

  if [ -f "${XDG_DESKTOP_DIR}/processing.desktop" ]; then
    rm "${XDG_DESKTOP_DIR}/processing.desktop"
  fi

  if [ -f "${XDG_DESKTOP_DIR}/${RESOURCE_NAME}.desktop" ]; then
    rm "${XDG_DESKTOP_DIR}/${RESOURCE_NAME}.desktop"
  fi

}

# Update desktop file and mime databases (if possible)
updatedbs_f() {

  if [ -d "${HOME}/.local/share/applications" ]; then
    if command -v update-desktop-database > /dev/null; then
      update-desktop-database "${HOME}/.local/share/applications"
    fi
  fi

  if [ -d "${HOME}/.local/share/mime" ]; then
    if command -v update-mime-database > /dev/null; then
      update-mime-database "${HOME}/.local/share/mime"
    fi
  fi

}

# Check availability of xdg-utils
xdg_exists_f() {

  if ! command -v xdg-icon-resource > /dev/null; then return 1; fi
  if ! command -v xdg-desktop-menu > /dev/null; then return 1; fi
  if ! command -v xdg-desktop-icon > /dev/null; then return 1; fi
  if ! command -v xdg-mime > /dev/null; then return 1; fi
  return 0

}

# Shows a description of the available options
display_help_f() {
  printf "\nThis script will add a Processing desktop shortcut, menu item,\n"
  printf "icons and file associations for the current user.\n"
  if ! xdg_exists_f; then
    printf "\nxdg-utils are recommended to be installed, so this script can use them.\n"
  fi
  printf "\nOptional arguments are:\n\n"
  printf "\t-u, --uninstall\t\tRemoves shortcut, menu item and icons.\n\n"
  printf "\t-h, --help\t\tShows this help again.\n\n"
}

# Check for provided arguments
while [ $# -gt 0 ] ; do
  ARG="${1}"
  case $ARG in
      -u|--uninstall)
        UNINSTALL=true
        shift
      ;;
      -h|--help)
        display_help_f
        exit 0
      ;;
      *)
        printf "\nInvalid option -- '${ARG}'\n"
        display_help_f
        exit 1
      ;;
  esac
done

# If possible, use xdg-utils, if not, use a more basic approach
if xdg_exists_f; then
  if [ ${UNINSTALL} = true ]; then
    printf "Removing desktop shortcut and menu item for Processing..."
    xdg_uninstall_f
    simple_uninstall_f
  else
    printf "Adding desktop shortcut, menu item and file associations for Processing..."
    xdg_uninstall_f
    simple_uninstall_f
    xdg_install_f
  fi
else
  if [ ${UNINSTALL} = true ]; then
    printf "Removing desktop shortcut and menu item for Processing..."
    simple_uninstall_f
  else
    printf "Adding desktop shortcut and menu item for Processing..."
    simple_uninstall_f
    simple_install_f
  fi
fi
updatedbs_f
printf " done!\n"

exit 0
